From dbac11d37dfb2ad8edd80b1be0444341ce34a591 Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Thu, 17 Sep 2009 19:20:33 +0000
Subject: [PATCH] JCR-2170: Remove PropDefId and NodeDefId - merged changes
 from sandbox branch

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@816343 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/BatchedItemOperations.java           |  84 ++--
 .../org/apache/jackrabbit/core/ItemImpl.java  |  25 +-
 .../apache/jackrabbit/core/ItemManager.java   | 132 +++---
 .../apache/jackrabbit/core/ItemValidator.java |  40 +-
 .../org/apache/jackrabbit/core/NodeImpl.java  | 143 ++++---
 .../core/NodeTypeInstanceHandler.java         |   4 +-
 .../apache/jackrabbit/core/PropertyImpl.java  |  16 +-
 .../core/ProtectedItemModifier.java           |   5 +-
 .../apache/jackrabbit/core/SessionImpl.java   |   5 +-
 .../jackrabbit/core/lock/LockManagerImpl.java |   7 -
 .../core/nodetype/EffectiveNodeType.java      | 355 ++++++++--------
 .../jackrabbit/core/nodetype/ItemDef.java     |  97 -----
 .../jackrabbit/core/nodetype/ItemDefImpl.java | 235 -----------
 .../core/nodetype/ItemDefinitionImpl.java     | 181 --------
 .../jackrabbit/core/nodetype/NodeDef.java     |  59 ---
 .../jackrabbit/core/nodetype/NodeDefId.java   |   5 +-
 .../jackrabbit/core/nodetype/NodeDefImpl.java | 273 ------------
 .../core/nodetype/NodeDefinitionImpl.java     | 188 ---------
 .../jackrabbit/core/nodetype/NodeTypeDef.java |  59 ++-
 .../core/nodetype/NodeTypeDefDiff.java        |  80 ++--
 .../core/nodetype/NodeTypeDefinitionImpl.java |   8 +-
 .../core/nodetype/NodeTypeImpl.java           |  38 +-
 .../core/nodetype/NodeTypeManagerImpl.java    |  58 ++-
 .../core/nodetype/NodeTypeRegistry.java       | 107 ++---
 .../jackrabbit/core/nodetype/PropDef.java     |  89 ----
 .../jackrabbit/core/nodetype/PropDefId.java   |  20 +-
 .../jackrabbit/core/nodetype/PropDefImpl.java | 388 ------------------
 .../core/nodetype/PropertyDefinitionImpl.java | 152 -------
 .../virtual/VirtualNodeTypeStateProvider.java |  22 +-
 .../core/nodetype/xml/NodeTypeReader.java     |  56 ++-
 .../core/nodetype/xml/NodeTypeWriter.java     |  22 +-
 .../core/persistence/PersistenceCopier.java   |   2 -
 .../AbstractBundlePersistenceManager.java     |  27 +-
 .../bundle/util/BundleBinding.java            |  12 +-
 .../bundle/util/ItemStateBinding.java         |   7 +-
 .../bundle/util/NodePropBundle.java           |  48 ---
 .../core/persistence/util/Serializer.java     |  16 +-
 .../xml/XMLPersistenceManager.java            |  13 -
 .../core/query/PropertyTypeRegistry.java      |   6 +-
 .../core/query/lucene/QueryImpl.java          |   7 +-
 .../query/lucene/QueryObjectModelImpl.java    |   4 +-
 .../jackrabbit/core/state/NodeState.java      |  25 --
 .../jackrabbit/core/state/PropertyState.java  |  26 +-
 .../core/state/SessionItemStateManager.java   |  22 +-
 .../core/state/SharedItemStateManager.java    |  75 ++--
 .../jackrabbit/core/value/InternalValue.java  |  12 +
 .../version/InternalVersionManagerImpl.java   |  12 -
 .../jackrabbit/core/version/NodeStateEx.java  |  48 ++-
 .../version/VersionManagerImplRestore.java    |  12 +-
 .../core/virtual/AbstractVISProvider.java     |  25 +-
 .../xml/DefaultProtectedPropertyImporter.java |  10 +-
 .../apache/jackrabbit/core/xml/PropInfo.java  |   8 +-
 .../core/xml/ProtectedPropertyImporter.java   |   8 +-
 .../jackrabbit/core/xml/SessionImporter.java  |   6 +-
 .../core/xml/WorkspaceImporter.java           |  17 +-
 .../CyclicNodeTypeRegistrationTest.java       |  32 +-
 .../jackrabbit/core/nodetype/xml/TestAll.java |  99 ++---
 .../spi/commons/QItemDefinitionImpl.java      |   7 -
 .../spi/commons/QNodeDefinitionImpl.java      |  12 +-
 .../spi/commons/QNodeTypeDefinitionImpl.java  | 248 ++++++++---
 .../commons/nodetype/NodeDefinitionImpl.java  |  10 +
 .../nodetype/PropertyDefinitionImpl.java      |  10 +
 .../nodetype/QDefinitionBuilderFactory.java   |   4 -
 .../nodetype/QItemDefinitionBuilder.java      |  14 +-
 .../nodetype/QNodeDefinitionBuilder.java      |  26 +-
 .../nodetype/QPropertyDefinitionBuilder.java  |   5 +-
 .../spi2jcr/QNodeDefinitionImpl.java          |   3 +-
 .../spi2jcr/QPropertyDefinitionImpl.java      |  25 +-
 68 files changed, 1087 insertions(+), 2809 deletions(-)
 delete mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/ItemDef.java
 delete mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/ItemDefImpl.java
 delete mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/ItemDefinitionImpl.java
 delete mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeDef.java
 delete mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeDefImpl.java
 delete mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeDefinitionImpl.java
 delete mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/PropDef.java
 delete mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/PropDefImpl.java
 delete mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/PropertyDefinitionImpl.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/BatchedItemOperations.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/BatchedItemOperations.java
index c56b2807a..527485447 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/BatchedItemOperations.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/BatchedItemOperations.java
@@ -18,7 +18,6 @@ package org.apache.jackrabbit.core;
 
 import java.util.ArrayList;
 import java.util.Arrays;
import java.util.Calendar;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Set;
@@ -40,11 +39,8 @@ import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.id.PropertyId;
 import org.apache.jackrabbit.core.lock.LockManager;
 import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeDef;
 import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
 import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropDefId;
 import org.apache.jackrabbit.core.security.AccessManager;
 import org.apache.jackrabbit.core.security.authorization.Permission;
 import org.apache.jackrabbit.core.state.ChildNodeEntry;
@@ -62,6 +58,9 @@ import org.apache.jackrabbit.core.version.VersionHistoryInfo;
 import org.apache.jackrabbit.core.version.InternalVersionManager;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
 import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
 import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
@@ -106,7 +105,8 @@ public class BatchedItemOperations extends ItemValidator {
                                  LockManager lockMgr,
                                  SessionImpl session,
                                  HierarchyManager hierMgr) throws RepositoryException {
        super(ntReg, hierMgr, session, lockMgr, session.getAccessManager(), session.getRetentionRegistry());
        super(ntReg, hierMgr, session, lockMgr, session.getAccessManager(),
                session.getRetentionRegistry(), session.getItemManager());
         this.stateMgr = stateMgr;
         this.session = session;
     }
@@ -432,12 +432,6 @@ public class BatchedItemOperations extends ItemValidator {
         // add to new parent
         destParentState.addChildNodeEntry(destName.getName(), newState.getNodeId());
 
        // change definition (id) of new node
        NodeDef newNodeDef =
                findApplicableNodeDefinition(destName.getName(),
                        srcState.getNodeTypeName(), destParentState);
        newState.setDefinitionId(newNodeDef.getId());

         // adjust references that refer to uuid's which have been mapped to
         // newly generated uuid's on copy/clone
         Iterator<Object> iter = refTracker.getProcessedReferences();
@@ -589,12 +583,6 @@ public class BatchedItemOperations extends ItemValidator {
             destParent.addChildNodeEntry(destName.getName(), target.getNodeId());
         }
 
        // change definition (id) of target node
        NodeDef newTargetDef =
                findApplicableNodeDefinition(destName.getName(),
                        target.getNodeTypeName(), destParent);
        target.setDefinitionId(newTargetDef.getId());

         // store states
         stateMgr.store(target);
         if (renameOnly) {
@@ -725,7 +713,7 @@ public class BatchedItemOperations extends ItemValidator {
         // 4. node type constraints
 
         if ((options & CHECK_CONSTRAINTS) == CHECK_CONSTRAINTS) {
            NodeDef parentDef = ntReg.getNodeDef(parentState.getDefinitionId());
            QItemDefinition parentDef = itemMgr.getDefinition(parentState).unwrap();
             // make sure parent node is not protected
             if (parentDef.isProtected()) {
                 throw new ConstraintViolationException(
@@ -735,7 +723,7 @@ public class BatchedItemOperations extends ItemValidator {
             // make sure there's an applicable definition for new child node
             EffectiveNodeType entParent = getEffectiveNodeType(parentState);
             entParent.checkAddNodeConstraints(nodeName, nodeTypeName, ntReg);
            NodeDef newNodeDef =
            QNodeDefinition newNodeDef =
                     findApplicableNodeDefinition(nodeName, nodeTypeName,
                             parentState);
 
@@ -756,8 +744,7 @@ public class BatchedItemOperations extends ItemValidator {
                     log.debug(msg);
                     throw new RepositoryException(msg, ise);
                 }
                NodeDef conflictingTargetDef =
                        ntReg.getNodeDef(conflictingState.getDefinitionId());
                QNodeDefinition conflictingTargetDef = itemMgr.getDefinition(conflictingState).unwrap();
                 // check same-name sibling setting of both target and existing node
                 if (!conflictingTargetDef.allowsSameNameSiblings()
                         || !newNodeDef.allowsSameNameSiblings()) {
@@ -900,12 +887,12 @@ public class BatchedItemOperations extends ItemValidator {
         // 4. node type constraints
 
         if ((options & CHECK_CONSTRAINTS) == CHECK_CONSTRAINTS) {
            NodeDef parentDef = ntReg.getNodeDef(parentState.getDefinitionId());
            QItemDefinition parentDef = itemMgr.getDefinition(parentState).unwrap();
             if (parentDef.isProtected()) {
                 throw new ConstraintViolationException(safeGetJCRPath(parentId)
                         + ": cannot remove child node of protected parent node");
             }
            NodeDef targetDef = ntReg.getNodeDef(targetState.getDefinitionId());
            QItemDefinition targetDef = itemMgr.getDefinition(targetState).unwrap();
             if (targetDef.isMandatory()) {
                 throw new ConstraintViolationException(safeGetJCRPath(targetPath)
                         + ": cannot remove mandatory node");
@@ -1071,7 +1058,7 @@ public class BatchedItemOperations extends ItemValidator {
                     + " because manager is not in edit mode");
         }
 
        NodeDef def = findApplicableNodeDefinition(nodeName, nodeTypeName, parent);
        QNodeDefinition def = findApplicableNodeDefinition(nodeName, nodeTypeName, parent);
         return createNodeState(parent, nodeName, nodeTypeName, mixinNames, id, def);
     }
 
@@ -1099,7 +1086,7 @@ public class BatchedItemOperations extends ItemValidator {
                                      Name nodeTypeName,
                                      Name[] mixinNames,
                                      NodeId id,
                                     NodeDef def)
                                     QNodeDefinition def)
             throws ItemExistsException, ConstraintViolationException,
             RepositoryException, IllegalStateException {
 
@@ -1128,7 +1115,6 @@ public class BatchedItemOperations extends ItemValidator {
         if (mixinNames != null && mixinNames.length > 0) {
             node.setMixinTypeNames(new HashSet<Name>(Arrays.asList(mixinNames)));
         }
        node.setDefinitionId(def.getId());
 
         // now add new child node entry to parent
         parent.addChildNodeEntry(nodeName, id);
@@ -1142,18 +1128,18 @@ public class BatchedItemOperations extends ItemValidator {
 
         if (!node.getMixinTypeNames().isEmpty()) {
             // create jcr:mixinTypes property
            PropDef pd = ent.getApplicablePropertyDef(NameConstants.JCR_MIXINTYPES,
            QPropertyDefinition pd = ent.getApplicablePropertyDef(NameConstants.JCR_MIXINTYPES,
                     PropertyType.NAME, true);
             createPropertyState(node, pd.getName(), pd.getRequiredType(), pd);
         }
 
         // add 'auto-create' properties defined in node type
        for (PropDef pd : ent.getAutoCreatePropDefs()) {
        for (QPropertyDefinition pd : ent.getAutoCreatePropDefs()) {
             createPropertyState(node, pd.getName(), pd.getRequiredType(), pd);
         }
 
         // recursively add 'auto-create' child nodes defined in node type
        for (NodeDef nd : ent.getAutoCreateNodeDefs()) {
        for (QNodeDefinition nd : ent.getAutoCreateNodeDefs()) {
             createNodeState(node, nd.getName(), nd.getDefaultPrimaryType(),
                     null, null, nd);
         }
@@ -1198,7 +1184,7 @@ public class BatchedItemOperations extends ItemValidator {
         }
 
         // find applicable definition
        PropDef def;
        QPropertyDefinition def;
         // multi- or single-valued property?
         if (numValues == 1) {
             // could be single- or multi-valued (n == 1)
@@ -1237,7 +1223,7 @@ public class BatchedItemOperations extends ItemValidator {
     public PropertyState createPropertyState(NodeState parent,
                                              Name propName,
                                              int type,
                                             PropDef def)
                                             QPropertyDefinition def)
             throws ItemExistsException, RepositoryException {
 
         // check for name collisions with existing properties
@@ -1249,7 +1235,6 @@ public class BatchedItemOperations extends ItemValidator {
         // create property
         PropertyState prop = stateMgr.createNew(propName, parent.getNodeId());
 
        prop.setDefinitionId(def.getId());
         if (def.getRequiredType() != PropertyType.UNDEFINED) {
             prop.setType(def.getRequiredType());
         } else if (type != PropertyType.UNDEFINED) {
@@ -1265,7 +1250,7 @@ public class BatchedItemOperations extends ItemValidator {
         if (genValues != null) {
             prop.setValues(genValues);
         } else if (def.getDefaultValues() != null) {
            prop.setValues(def.getDefaultValues());
            prop.setValues(InternalValue.create(def.getDefaultValues()));
         }
 
         // now add new property entry to parent
@@ -1442,8 +1427,7 @@ public class BatchedItemOperations extends ItemValidator {
             throws PathNotFoundException, ConstraintViolationException,
             RepositoryException {
         NodeState node = getNodeState(nodePath);
        NodeDef parentDef = ntReg.getNodeDef(node.getDefinitionId());
        if (parentDef.isProtected()) {
        if (itemMgr.getDefinition(node).isProtected()) {
             throw new ConstraintViolationException(safeGetJCRPath(nodePath)
                     + ": node is protected");
         }
@@ -1686,7 +1670,6 @@ public class BatchedItemOperations extends ItemValidator {
             newState = stateMgr.createNew(id, srcState.getNodeTypeName(), destParentId);
             // copy node state
             newState.setMixinTypeNames(srcState.getMixinTypeNames());
            newState.setDefinitionId(srcState.getDefinitionId());
             if (shareable) {
                 // initialize shared set
                 newState.addShare(destParentId);
@@ -1773,15 +1756,16 @@ public class BatchedItemOperations extends ItemValidator {
                  *
                  * todo FIXME delegate to 'node type instance handler'
                  */
                PropDefId defId = srcChildState.getDefinitionId();
                PropDef def = ntReg.getPropDef(defId);
                QPropertyDefinition def = ent.getApplicablePropertyDef(
                        srcChildState.getName(), srcChildState.getType(),
                        srcChildState.isMultiValued());
                 if (def.getDeclaringNodeType().equals(NameConstants.MIX_LOCKABLE)) {
                     // skip properties defined by mix:lockable
                     continue;
                 }
 
                 PropertyState newChildState =
                        copyPropertyState(srcChildState, id, propName);
                        copyPropertyState(srcChildState, id, propName, def);
 
                 if (history != null) {
                     if (fullVersionable) {
@@ -1830,23 +1814,21 @@ public class BatchedItemOperations extends ItemValidator {
     /**
      * Copies the specified property state.
      *
     * @param srcState
     * @param parentId
     * @param propName
     * @return
     * @throws RepositoryException
     * @param srcState the property state to copy.
     * @param parentId the id of the parent node.
     * @param propName the name of the property.
     * @param def      the definition of the property.
     * @return a copy of the property state.
     * @throws RepositoryException if an error occurs while copying.
      */
     private PropertyState copyPropertyState(PropertyState srcState,
                                             NodeId parentId,
                                            Name propName)
                                            Name propName,
                                            QPropertyDefinition def)
             throws RepositoryException {
 
        PropDefId defId = srcState.getDefinitionId();
        PropDef def = ntReg.getPropDef(defId);

         PropertyState newState = stateMgr.createNew(propName, parentId);
 
        newState.setDefinitionId(defId);
         newState.setType(srcState.getType());
         newState.setMultiValued(srcState.isMultiValued());
         InternalValue[] values = srcState.getValues();
@@ -1858,8 +1840,8 @@ public class BatchedItemOperations extends ItemValidator {
              *
              * todo FIXME delegate to 'node type instance handler'
              */
            if (def.getDeclaringNodeType().equals(NameConstants.MIX_REFERENCEABLE)
                    && propName.equals(NameConstants.JCR_UUID)) {
            if (propName.equals(NameConstants.JCR_UUID)
                    && def.getDeclaringNodeType().equals(NameConstants.MIX_REFERENCEABLE)) {
                 // set correct value of jcr:uuid property
                 newState.setValues(new InternalValue[]{InternalValue.create(parentId.toString())});
             } else {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemImpl.java
index b72bfd7e8..a50b992aa 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemImpl.java
@@ -48,13 +48,10 @@ import javax.jcr.version.VersionException;
 import org.apache.jackrabbit.core.id.ItemId;
 import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeDef;
 import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
 import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
 import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
 import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropertyDefinitionImpl;
 import org.apache.jackrabbit.core.security.AccessManager;
 import org.apache.jackrabbit.core.security.authorization.Permission;
 import org.apache.jackrabbit.core.state.ChildNodeEntry;
@@ -69,6 +66,8 @@ import org.apache.jackrabbit.core.version.VersionHistoryInfo;
 import org.apache.jackrabbit.core.version.InternalVersionManager;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QItemDefinition;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
 import org.apache.jackrabbit.util.Text;
 import org.slf4j.Logger;
@@ -373,9 +372,9 @@ public abstract class ItemImpl implements Item {
         for (ItemState itemState : dirty) {
             ItemDefinition def;
             if (itemState.isNode()) {
                def = ntMgr.getNodeDefinition(((NodeState) itemState).getDefinitionId());
                def = itemMgr.getDefinition((NodeState) itemState);
             } else {
                def = ntMgr.getPropertyDefinition(((PropertyState) itemState).getDefinitionId());
                def = itemMgr.getDefinition((PropertyState) itemState);
             }
             /* check permissions for non-protected items. protected items are
                only added through API methods which need to assert that
@@ -444,9 +443,9 @@ public abstract class ItemImpl implements Item {
                 }
 
                 // mandatory child properties
                PropDef[] pda = ent.getMandatoryPropDefs();
                QPropertyDefinition[] pda = ent.getMandatoryPropDefs();
                 for (int i = 0; i < pda.length; i++) {
                    PropDef pd = pda[i];
                    QPropertyDefinition pd = pda[i];
                     if (pd.getDeclaringNodeType().equals(NameConstants.MIX_VERSIONABLE)
                             || pd.getDeclaringNodeType().equals(NameConstants.MIX_SIMPLE_VERSIONABLE)) {
                         /**
@@ -465,9 +464,9 @@ public abstract class ItemImpl implements Item {
                     }
                 }
                 // mandatory child nodes
                NodeDef[] cnda = ent.getMandatoryNodeDefs();
                QItemDefinition[] cnda = ent.getMandatoryNodeDefs();
                 for (int i = 0; i < cnda.length; i++) {
                    NodeDef cnd = cnda[i];
                    QItemDefinition cnd = cnda[i];
                     if (!nodeState.hasChildNodeEntry(cnd.getName())) {
                         String msg = itemMgr.safeGetJCRPath(id)
                                 + ": mandatory child node " + cnd.getName()
@@ -480,7 +479,7 @@ public abstract class ItemImpl implements Item {
                 // the transient item is a property
                 PropertyState propState = (PropertyState) itemState;
                 ItemId propId = propState.getPropertyId();
                PropertyDefinitionImpl propDef = (PropertyDefinitionImpl) def;
                org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl propDef = (org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl) def;
 
                 /**
                  * check value constraints
@@ -579,11 +578,11 @@ public abstract class ItemImpl implements Item {
 
         // walk through list of removed transient items and check REMOVE permission
         for (ItemState itemState : removed) {
            ItemDefinition def;
            QItemDefinition def;
             if (itemState.isNode()) {
                def = ntMgr.getNodeDefinition(((NodeState) itemState).getDefinitionId());
                def = itemMgr.getDefinition((NodeState) itemState).unwrap();
             } else {
                def = ntMgr.getPropertyDefinition(((PropertyState) itemState).getDefinitionId());
                def = itemMgr.getDefinition((PropertyState) itemState).unwrap();
             }
             if (!def.isProtected()) {
                 Path path = stateMgr.getAtticAwareHierarchyMgr().getPath(itemState.getId());
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemManager.java
index a96fcd5bd..e04e67b8a 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemManager.java
@@ -29,23 +29,19 @@ import javax.jcr.NodeIterator;
 import javax.jcr.PathNotFoundException;
 import javax.jcr.PropertyIterator;
 import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
 
 import org.apache.commons.collections.map.ReferenceMap;
 import org.apache.jackrabbit.core.id.ItemId;
 import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.NodeDefinitionImpl;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.nodetype.PropertyDefinitionImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
 import org.apache.jackrabbit.core.security.AccessManager;
 import org.apache.jackrabbit.core.state.ChildNodeEntry;
 import org.apache.jackrabbit.core.state.ItemState;
 import org.apache.jackrabbit.core.state.ItemStateException;
 import org.apache.jackrabbit.core.state.ItemStateListener;
import org.apache.jackrabbit.core.state.ItemStateManager;
 import org.apache.jackrabbit.core.state.NoSuchItemStateException;
 import org.apache.jackrabbit.core.state.NodeState;
 import org.apache.jackrabbit.core.state.PropertyState;
@@ -55,6 +51,8 @@ import org.apache.jackrabbit.core.version.VersionHistoryImpl;
 import org.apache.jackrabbit.core.version.VersionImpl;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -86,12 +84,12 @@ public class ItemManager implements Dumpable, ItemStateListener {
 
     private static Logger log = LoggerFactory.getLogger(ItemManager.class);
 
    private final NodeDefinition rootNodeDef;
    private final org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl rootNodeDef;
     private final NodeId rootNodeId;
 
     protected final SessionImpl session;
 
    private final ItemStateManager itemStateProvider;
    private final SessionItemStateManager sism;
     private final HierarchyManager hierMgr;
 
     /**
@@ -107,17 +105,19 @@ public class ItemManager implements Dumpable, ItemStateListener {
     /**
      * Creates a new per-session instance <code>ItemManager</code> instance.
      *
     * @param itemStateProvider the item state provider associated with
     *                          the new instance
     * @param hierMgr           the hierarchy manager
     * @param session           the session associated with the new instance
     * @param rootNodeDef       the definition of the root node
     * @param rootNodeId        the id of the root node
     * @param sism        the item state manager associated with the new
     *                    instance
     * @param hierMgr     the hierarchy manager
     * @param session     the session associated with the new instance
     * @param rootNodeDef the definition of the root node
     * @param rootNodeId  the id of the root node
      */
    protected ItemManager(SessionItemStateManager itemStateProvider, HierarchyManager hierMgr,
                          SessionImpl session, NodeDefinition rootNodeDef,
    protected ItemManager(SessionItemStateManager sism,
                          HierarchyManager hierMgr,
                          SessionImpl session,
                          org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl rootNodeDef,
                           NodeId rootNodeId) {
        this.itemStateProvider = itemStateProvider;
        this.sism = sism;
         this.hierMgr = hierMgr;
         this.session = session;
         this.rootNodeDef = rootNodeDef;
@@ -145,7 +145,7 @@ public class ItemManager implements Dumpable, ItemStateListener {
             SessionItemStateManager itemStateProvider,
             HierarchyManager hierMgr,
             SessionImpl session,
            NodeDefinition rootNodeDef,
            org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl rootNodeDef,
             NodeId rootNodeId) {
         ItemManager mgr = new ItemManager(itemStateProvider, hierMgr,
                 session, rootNodeDef, rootNodeId);
@@ -163,52 +163,76 @@ public class ItemManager implements Dumpable, ItemStateListener {
         shareableNodesCache.clear();
     }
 
    NodeDefinition getDefinition(NodeState state)
    org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl getDefinition(NodeState state)
             throws RepositoryException {
         if (state.getId().equals(rootNodeId)) {
             // special handling required for root node
             return rootNodeDef;
         }
 
        NodeDefId defId = state.getDefinitionId();
        NodeDefinitionImpl def = session.getNodeTypeManager().getNodeDefinition(defId);
        if (def == null) {
            /**
             * todo need proper way of handling inconsistent/corrupt definition
             * e.g. 'flag' items that refer to non-existent definitions
             */
            log.warn("node at " + safeGetJCRPath(state.getNodeId())
                    + " has invalid definitionId (" + defId + ")");

            // fallback: try finding applicable definition
            NodeImpl parent = (NodeImpl) getItem(state.getParentId());
            NodeState parentState = parent.getNodeState();
            ChildNodeEntry cne = parentState.getChildNodeEntry(state.getNodeId());
            def = parent.getApplicableChildNodeDefinition(cne.getName(), state.getNodeTypeName());
            state.setDefinitionId(def.unwrap().getId());
        NodeId parentId = state.getParentId();
        if (parentId == null) {
            // removed state has parentId set to null
            // get from overlayed state
            parentId = state.getOverlayedState().getParentId();
        }
        NodeState parentState;
        try {
            NodeImpl parent = (NodeImpl) getItem(parentId);
            parentState = parent.getNodeState();
            if (state.getParentId() == null) {
                // indicates state has been removed, must use
                // overlayed state of parent, otherwise child node entry
                // cannot be found
                parentState = (NodeState) parentState.getOverlayedState();
            }
        } catch (ItemNotFoundException e) {
            // parent probably removed, get it from attic
            try {
                // use overlayed state if available
                parentState = (NodeState) sism.getAttic().getItemState(
                        parentId).getOverlayedState();
            } catch (ItemStateException ex) {
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
                    cne.getName(), state.getNodeTypeName(), ntReg);
            return session.getNodeTypeManager().getNodeDefinition(def);
        } catch (NodeTypeConflictException e) {
            throw new RepositoryException(e);
         }
        return def;
     }
 
    PropertyDefinition getDefinition(PropertyState state)
    org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl getDefinition(PropertyState state)
             throws RepositoryException {
        PropDefId defId = state.getDefinitionId();
        PropertyDefinitionImpl def = session.getNodeTypeManager().getPropertyDefinition(defId);
        if (def == null) {
            /**
             * todo need proper way of handling inconsistent/corrupt definition
             * e.g. 'flag' items that refer to non-existent definitions
             */
            log.warn("property at " + safeGetJCRPath(state.getPropertyId())
                    + " has invalid definitionId (" + defId + ")");

            // fallback: try finding applicable definition
        try {
             NodeImpl parent = (NodeImpl) getItem(state.getParentId());
            def = parent.getApplicablePropertyDefinition(
            return parent.getApplicablePropertyDefinition(
                     state.getName(), state.getType(), state.isMultiValued(), true);
            state.setDefinitionId(def.unwrap().getId());
        } catch (ItemNotFoundException e) {
            // parent probably removed, get it from attic
        }
        try {
            NodeState parent = (NodeState) sism.getAttic().getItemState(
                    state.getParentId()).getOverlayedState();
            NodeTypeRegistry ntReg = session.getNodeTypeManager().getNodeTypeRegistry();
            EffectiveNodeType ent = ntReg.getEffectiveNodeType(
                    parent.getNodeTypeName(), parent.getMixinTypeNames());
            QPropertyDefinition def = ent.getApplicablePropertyDef(
                    state.getName(), state.getType(), state.isMultiValued());
            return session.getNodeTypeManager().getPropertyDefinition(def);
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } catch (NodeTypeConflictException e) {
            throw new RepositoryException(e);
         }
        return def;
     }
 
     /**
@@ -228,7 +252,7 @@ public class ItemManager implements Dumpable, ItemStateListener {
             session.sanityCheck();
 
             // shortcut: check if state exists for the given item
            if (!itemStateProvider.hasItemState(itemId)) {
            if (!sism.hasItemState(itemId)) {
                 return false;
             }
             getItemData(itemId, path, true);
@@ -310,7 +334,7 @@ public class ItemManager implements Dumpable, ItemStateListener {
             // NOTE: permission check & caching within createItemData
             ItemState state;
             try {
                state = itemStateProvider.getItemState(itemId);
                state = sism.getItemState(itemId);
             } catch (NoSuchItemStateException nsise) {
                 throw new ItemNotFoundException(itemId.toString());
             } catch (ItemStateException ise) {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemValidator.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemValidator.java
index 3ba65ea2e..16a1bcde7 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemValidator.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ItemValidator.java
@@ -31,10 +31,8 @@ import javax.jcr.version.VersionException;
 import org.apache.jackrabbit.core.id.ItemId;
 import org.apache.jackrabbit.core.lock.LockManager;
 import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeDef;
 import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
 import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
 import org.apache.jackrabbit.core.retention.RetentionRegistry;
 import org.apache.jackrabbit.core.security.AccessManager;
 import org.apache.jackrabbit.core.security.authorization.Permission;
@@ -43,6 +41,9 @@ import org.apache.jackrabbit.core.state.PropertyState;
 import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
 import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -130,6 +131,8 @@ public class ItemValidator {
 
     protected final RetentionRegistry retentionReg;
 
    protected final ItemManager itemMgr;

     /**
      * Creates a new <code>ItemValidator</code> instance.
      *
@@ -140,7 +143,9 @@ public class ItemValidator {
     public ItemValidator(NodeTypeRegistry ntReg,
                          HierarchyManager hierMgr,
                          SessionImpl session) throws RepositoryException {
        this(ntReg, hierMgr, session, session.getLockManager(), session.getAccessManager(), session.getRetentionRegistry());
        this(ntReg, hierMgr, session, session.getLockManager(),
                session.getAccessManager(), session.getRetentionRegistry(),
                session.getItemManager());
     }
 
     /**
@@ -152,19 +157,22 @@ public class ItemValidator {
      * @param lockMgr    lockMgr
      * @param accessMgr  accessMgr
      * @param retentionReg
     * @param itemMgr    the item manager
      */
     public ItemValidator(NodeTypeRegistry ntReg,
                          HierarchyManager hierMgr,
                          PathResolver resolver,
                          LockManager lockMgr,
                          AccessManager accessMgr,
                         RetentionRegistry retentionReg) {
                         RetentionRegistry retentionReg,
                         ItemManager itemMgr) {
         this.ntReg = ntReg;
         this.hierMgr = hierMgr;
         this.resolver = resolver;
         this.lockMgr = lockMgr;
         this.accessMgr = accessMgr;
         this.retentionReg = retentionReg;
        this.itemMgr = itemMgr;
     }
 
     /**
@@ -190,7 +198,7 @@ public class ItemValidator {
                 ntReg.getEffectiveNodeType(nodeState.getNodeTypeName());
         // effective node type (primary type incl. mixins)
         EffectiveNodeType entPrimaryAndMixins = getEffectiveNodeType(nodeState);
        NodeDef def = ntReg.getNodeDef(nodeState.getDefinitionId());
        QNodeDefinition def = itemMgr.getDefinition(nodeState).unwrap();
 
         // check if primary type satisfies the 'required node types' constraint
         Name[] requiredPrimaryTypes = def.getRequiredPrimaryTypes();
@@ -204,9 +212,9 @@ public class ItemValidator {
             }
         }
         // mandatory properties
        PropDef[] pda = entPrimaryAndMixins.getMandatoryPropDefs();
        QPropertyDefinition[] pda = entPrimaryAndMixins.getMandatoryPropDefs();
         for (int i = 0; i < pda.length; i++) {
            PropDef pd = pda[i];
            QPropertyDefinition pd = pda[i];
             if (!nodeState.hasPropertyName(pd.getName())) {
                 String msg = safeGetJCRPath(nodeState.getNodeId())
                         + ": mandatory property " + pd.getName()
@@ -216,9 +224,9 @@ public class ItemValidator {
             }
         }
         // mandatory child nodes
        NodeDef[] cnda = entPrimaryAndMixins.getMandatoryNodeDefs();
        QItemDefinition[] cnda = entPrimaryAndMixins.getMandatoryNodeDefs();
         for (int i = 0; i < cnda.length; i++) {
            NodeDef cnd = cnda[i];
            QItemDefinition cnd = cnda[i];
             if (!nodeState.hasChildNodeEntry(cnd.getName())) {
                 String msg = safeGetJCRPath(nodeState.getNodeId())
                         + ": mandatory child node " + cnd.getName()
@@ -246,7 +254,7 @@ public class ItemValidator {
      */
     public void validate(PropertyState propState)
             throws ConstraintViolationException, RepositoryException {
        PropDef def = ntReg.getPropDef(propState.getDefinitionId());
        QPropertyDefinition def = itemMgr.getDefinition(propState).unwrap();
         InternalValue[] values = propState.getValues();
         int type = PropertyType.UNDEFINED;
         for (int i = 0; i < values.length; i++) {
@@ -451,12 +459,12 @@ public class ItemValidator {
      * @param name
      * @param nodeTypeName
      * @param parentState
     * @return a <code>NodeDef</code>
     * @return a <code>QNodeDefinition</code>
      * @throws ConstraintViolationException if no applicable child node definition
      *                                      could be found
      * @throws RepositoryException          if another error occurs
      */
    public NodeDef findApplicableNodeDefinition(Name name,
    public QNodeDefinition findApplicableNodeDefinition(Name name,
                                                 Name nodeTypeName,
                                                 NodeState parentState)
             throws RepositoryException, ConstraintViolationException {
@@ -479,12 +487,12 @@ public class ItemValidator {
      * @param type
      * @param multiValued
      * @param parentState
     * @return a <code>PropDef</code>
     * @return a <code>QPropertyDefinition</code>
      * @throws ConstraintViolationException if no applicable property definition
      *                                      could be found
      * @throws RepositoryException          if another error occurs
      */
    public PropDef findApplicablePropertyDefinition(Name name,
    public QPropertyDefinition findApplicablePropertyDefinition(Name name,
                                                     int type,
                                                     boolean multiValued,
                                                     NodeState parentState)
@@ -510,12 +518,12 @@ public class ItemValidator {
      * @param name
      * @param type
      * @param parentState
     * @return a <code>PropDef</code>
     * @return a <code>QPropertyDefinition</code>
      * @throws ConstraintViolationException if no applicable property definition
      *                                      could be found
      * @throws RepositoryException          if another error occurs
      */
    public PropDef findApplicablePropertyDefinition(Name name,
    public QPropertyDefinition findApplicablePropertyDefinition(Name name,
                                                     int type,
                                                     NodeState parentState)
             throws RepositoryException, ConstraintViolationException {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java
index 6520ce8c2..b897277ea 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java
@@ -71,16 +71,10 @@ import org.apache.jackrabbit.core.id.ItemId;
 import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.id.PropertyId;
 import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.ItemDef;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.NodeDefinitionImpl;
 import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
 import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
 import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
 import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropertyDefinitionImpl;
 import org.apache.jackrabbit.core.query.QueryManagerImpl;
 import org.apache.jackrabbit.core.security.authorization.Permission;
 import org.apache.jackrabbit.core.state.ChildNodeEntry;
@@ -92,6 +86,9 @@ import org.apache.jackrabbit.core.state.PropertyState;
 import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
 import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
 import org.apache.jackrabbit.spi.commons.conversion.NameException;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
@@ -108,6 +105,8 @@ import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_LIFECYCLE
 import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_CURRENT_LIFECYCLE_STATE;
 import static org.apache.jackrabbit.spi.commons.name.NameConstants.MIX_LIFECYCLE;
 import static org.apache.jackrabbit.spi.commons.name.NameConstants.MIX_REFERENCEABLE;
import org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl;
 
 /**
  * <code>NodeImpl</code> implements the <code>Node</code> interface.
@@ -144,6 +143,23 @@ public class NodeImpl extends ItemImpl implements Node {
                     + state.getNodeTypeName() + "' of " + this);
             data.getNodeState().setNodeTypeName(NameConstants.NT_UNSTRUCTURED);
         }
        List<Name> unknown = null;
        for (Name mixinName : state.getMixinTypeNames()) {
            if (!ntReg.isRegistered(mixinName)) {
                if (unknown == null) {
                    unknown = new ArrayList<Name>();
                }
                unknown.add(mixinName);
                log.warn("Ignoring unknown mixin type '" + mixinName +
                        "' of " + this);
            }
        }
        if (unknown != null) {
            // ignore unknown mixin type names
            Set<Name> known = new HashSet<Name>(state.getMixinTypeNames());
            known.removeAll(unknown);
            state.setMixinTypeNames(known);
        }
     }
 
     /**
@@ -359,7 +375,7 @@ public class NodeImpl extends ItemImpl implements Node {
         if (isNew() && !hasProperty(name)) {
             // this is a new node and the property does not exist yet
             // -> no need to check item manager
            PropertyDefinitionImpl def = getApplicablePropertyDefinition(
            org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl def = getApplicablePropertyDefinition(
                     name, type, multiValued, exactTypeMatch);
             PropertyImpl prop = createChildProperty(name, type, def);
             status.set(CREATED);
@@ -396,7 +412,7 @@ public class NodeImpl extends ItemImpl implements Node {
         } catch (ItemNotFoundException e) {
             // does not exist yet:
             // find definition for the specified property and create property
            PropertyDefinitionImpl def = getApplicablePropertyDefinition(
            org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl def = getApplicablePropertyDefinition(
                     name, type, multiValued, exactTypeMatch);
             PropertyImpl prop = createChildProperty(name, type, def);
             status.set(CREATED);
@@ -405,24 +421,23 @@ public class NodeImpl extends ItemImpl implements Node {
     }
 
     protected synchronized PropertyImpl createChildProperty(Name name, int type,
                                                            PropertyDefinitionImpl def)
                                                            org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl def)
             throws RepositoryException {
 
         // create a new property state
         PropertyState propState;
         try {
            PropDef propDef = def.unwrap();
            QPropertyDefinition propDef = def.unwrap();
             propState =
                     stateMgr.createTransientPropertyState(getNodeId(), name,
                             ItemState.STATUS_NEW);
             propState.setType(type);
             propState.setMultiValued(propDef.isMultiple());
            propState.setDefinitionId(propDef.getId());
             // compute system generated values if necessary
             InternalValue[] genValues = session.getNodeTypeInstanceHandler()
                     .computeSystemGeneratedPropertyValues(data.getNodeState(), propDef);
             if (genValues == null) {
                genValues = propDef.getDefaultValues();
                genValues = InternalValue.create(propDef.getDefaultValues());
             }
             if (genValues != null) {
                 propState.setValues(genValues);
@@ -450,7 +465,6 @@ public class NodeImpl extends ItemImpl implements Node {
     }
 
     protected synchronized NodeImpl createChildNode(Name name,
                                                    NodeDefinitionImpl def,
                                                     NodeTypeImpl nodeType,
                                                     NodeId id)
             throws RepositoryException {
@@ -463,7 +477,6 @@ public class NodeImpl extends ItemImpl implements Node {
             nodeState =
                     stateMgr.createTransientNodeState(id, nodeType.getQName(),
                             getNodeId(), ItemState.STATUS_NEW);
            nodeState.setDefinitionId(def.unwrap().getId());
         } catch (ItemStateException ise) {
             String msg = "failed to add child node " + name + " to " + this;
             log.debug(msg);
@@ -495,15 +508,14 @@ public class NodeImpl extends ItemImpl implements Node {
         PropertyDefinition[] pda = nodeType.getAutoCreatedPropertyDefinitions();
         for (int i = 0; i < pda.length; i++) {
             PropertyDefinitionImpl pd = (PropertyDefinitionImpl) pda[i];
            node.createChildProperty(pd.getQName(), pd.getRequiredType(), pd);
            node.createChildProperty(pd.unwrap().getName(), pd.getRequiredType(), pd);
         }
 
         // recursively add 'auto-create' child nodes defined in node type
         NodeDefinition[] nda = nodeType.getAutoCreatedNodeDefinitions();
         for (int i = 0; i < nda.length; i++) {
             NodeDefinitionImpl nd = (NodeDefinitionImpl) nda[i];
            node.createChildNode(nd.getQName(), nd,
                    (NodeTypeImpl) nd.getDefaultPrimaryType(), null);
            node.createChildNode(nd.unwrap().getName(), (NodeTypeImpl) nd.getDefaultPrimaryType(), null);
         }
 
         return node;
@@ -570,13 +582,12 @@ public class NodeImpl extends ItemImpl implements Node {
         }
     }
 
    protected void onRedefine(NodeDefId defId) throws RepositoryException {
        NodeDefinitionImpl newDef =
                session.getNodeTypeManager().getNodeDefinition(defId);
    protected void onRedefine(QNodeDefinition def) throws RepositoryException {
        org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl newDef =
                session.getNodeTypeManager().getNodeDefinition(def);
         // modify the state of 'this', i.e. the target node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();
        // set id of new definition
        thisState.setDefinitionId(defId);
        getOrCreateTransientItemState();
        // set new definition
         data.setDefinition(newDef);
     }
 
@@ -641,7 +652,7 @@ public class NodeImpl extends ItemImpl implements Node {
             prop = (PropertyImpl) itemMgr.getItem(new PropertyId(thisState.getNodeId(), NameConstants.JCR_MIXINTYPES));
         } else {
             // find definition for the jcr:mixinTypes property and create property
            PropertyDefinitionImpl def = getApplicablePropertyDefinition(
            org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl def = getApplicablePropertyDefinition(
                     NameConstants.JCR_MIXINTYPES, PropertyType.NAME, true, true);
             prop = createChildProperty(NameConstants.JCR_MIXINTYPES, PropertyType.NAME, def);
         }
@@ -704,13 +715,13 @@ public class NodeImpl extends ItemImpl implements Node {
      *                                      could be found
      * @throws RepositoryException          if another error occurs
      */
    protected NodeDefinitionImpl getApplicableChildNodeDefinition(Name nodeName,
    protected org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl getApplicableChildNodeDefinition(Name nodeName,
                                                                   Name nodeTypeName)
             throws ConstraintViolationException, RepositoryException {
         NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
        NodeDef cnd = getEffectiveNodeType().getApplicableChildNodeDef(
        QNodeDefinition cnd = getEffectiveNodeType().getApplicableChildNodeDef(
                 nodeName, nodeTypeName, ntMgr.getNodeTypeRegistry());
        return ntMgr.getNodeDefinition(cnd.getId());
        return ntMgr.getNodeDefinition(cnd);
     }
 
     /**
@@ -726,12 +737,12 @@ public class NodeImpl extends ItemImpl implements Node {
      *                                      could be found
      * @throws RepositoryException          if another error occurs
      */
    protected PropertyDefinitionImpl getApplicablePropertyDefinition(Name propertyName,
    protected org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl getApplicablePropertyDefinition(Name propertyName,
                                                                      int type,
                                                                      boolean multiValued,
                                                                      boolean exactTypeMatch)
             throws ConstraintViolationException, RepositoryException {
        PropDef pd;
        QPropertyDefinition pd;
         if (exactTypeMatch || type == PropertyType.UNDEFINED) {
             pd = getEffectiveNodeType().getApplicablePropertyDef(
                     propertyName, type, multiValued);
@@ -746,7 +757,7 @@ public class NodeImpl extends ItemImpl implements Node {
                         propertyName, PropertyType.UNDEFINED, multiValued);
             }
         }
        return session.getNodeTypeManager().getPropertyDefinition(pd.getId());
        return session.getNodeTypeManager().getPropertyDefinition(pd);
     }
 
     protected void makePersistent() throws InvalidItemStateException {
@@ -779,8 +790,6 @@ public class NodeImpl extends ItemImpl implements Node {
             persistentState.setNodeTypeName(transientState.getNodeTypeName());
             // mixin types
             persistentState.setMixinTypeNames(transientState.getMixinTypeNames());
            // id of definition
            persistentState.setDefinitionId(transientState.getDefinitionId());
             // child node entries
             persistentState.setChildNodeEntries(transientState.getChildNodeEntries());
             // property entries
@@ -816,7 +825,6 @@ public class NodeImpl extends ItemImpl implements Node {
         thisState.setParentId(transientState.getParentId());
         thisState.setNodeTypeName(transientState.getNodeTypeName());
         thisState.setMixinTypeNames(transientState.getMixinTypeNames());
        thisState.setDefinitionId(transientState.getDefinitionId());
         thisState.setChildNodeEntries(transientState.getChildNodeEntries());
         thisState.setPropertyNames(transientState.getPropertyNames());
         thisState.setSharedSet(transientState.getSharedSet());
@@ -902,7 +910,7 @@ public class NodeImpl extends ItemImpl implements Node {
                 // or existing mixin's
                 NodeTypeImpl declaringNT = (NodeTypeImpl) pd.getDeclaringNodeType();
                 if (!entExisting.includesNodeType(declaringNT.getQName())) {
                    createChildProperty(pd.getQName(), pd.getRequiredType(), pd);
                    createChildProperty(pd.unwrap().getName(), pd.getRequiredType(), pd);
                 }
             }
 
@@ -914,7 +922,7 @@ public class NodeImpl extends ItemImpl implements Node {
                 // or existing mixin's
                 NodeTypeImpl declaringNT = (NodeTypeImpl) nd.getDeclaringNodeType();
                 if (!entExisting.includesNodeType(declaringNT.getQName())) {
                    createChildNode(nd.getQName(), nd, (NodeTypeImpl) nd.getDefaultPrimaryType(), null);
                    createChildNode(nd.unwrap().getName(), (NodeTypeImpl) nd.getDefaultPrimaryType(), null);
                 }
             }
         } catch (RepositoryException re) {
@@ -1021,7 +1029,7 @@ public class NodeImpl extends ItemImpl implements Node {
             for (Name propName : set) {
                 PropertyState propState = (PropertyState) stateMgr.getItemState(new PropertyId(thisState.getNodeId(), propName));
                 // check if property has been defined by mixin type (or one of its supertypes)
                PropertyDefinition def = ntMgr.getPropertyDefinition(propState.getDefinitionId());
                PropertyDefinition def = itemMgr.getDefinition(propState);
                 NodeTypeImpl declaringNT = (NodeTypeImpl) def.getDeclaringNodeType();
                 if (!entResulting.includesNodeType(declaringNT.getQName())) {
                     // the resulting effective node type doesn't include the
@@ -1036,7 +1044,7 @@ public class NodeImpl extends ItemImpl implements Node {
                             removeChildProperty(propName);
                             continue;
                         }
                        PropertyDefinitionImpl pdi = getApplicablePropertyDefinition(
                        org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl pdi = getApplicablePropertyDefinition(
                                 propName, propState.getType(),
                                 propState.isMultiValued(), false);
                         if (pdi.getRequiredType() != PropertyType.UNDEFINED
@@ -1050,7 +1058,7 @@ public class NodeImpl extends ItemImpl implements Node {
                                                 pdi.getRequiredType(),
                                                 session.getValueFactory());
                                 // redefine property
                                prop.onRedefine(pdi.unwrap().getId());
                                prop.onRedefine(pdi.unwrap());
                                 // set converted values
                                 prop.setValue(values);
                             } else {
@@ -1061,13 +1069,13 @@ public class NodeImpl extends ItemImpl implements Node {
                                                 pdi.getRequiredType(),
                                                 session.getValueFactory());
                                 // redefine property
                                prop.onRedefine(pdi.unwrap().getId());
                                prop.onRedefine(pdi.unwrap());
                                 // set converted values
                                 prop.setValue(value);
                             }
                         } else {
                             // redefine property
                            prop.onRedefine(pdi.unwrap().getId());
                            prop.onRedefine(pdi.unwrap());
                         }
                     } catch (ValueFormatException vfe) {
                         // value conversion failed, remove it
@@ -1085,7 +1093,7 @@ public class NodeImpl extends ItemImpl implements Node {
             for (int i = list.size() - 1; i >= 0; i--) {
                 ChildNodeEntry entry = list.get(i);
                 NodeState nodeState = (NodeState) stateMgr.getItemState(entry.getId());
                NodeDefinition def = ntMgr.getNodeDefinition(nodeState.getDefinitionId());
                NodeDefinition def = itemMgr.getDefinition(nodeState);
                 // check if node has been defined by mixin type (or one of its supertypes)
                 NodeTypeImpl declaringNT = (NodeTypeImpl) def.getDeclaringNodeType();
                 if (!entResulting.includesNodeType(declaringNT.getQName())) {
@@ -1099,11 +1107,11 @@ public class NodeImpl extends ItemImpl implements Node {
                             removeChildNode(entry.getName(), entry.getIndex());
                             continue;
                         }
                        NodeDefinitionImpl ndi = getApplicableChildNodeDefinition(
                        org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl ndi = getApplicableChildNodeDefinition(
                                 entry.getName(),
                                 nodeState.getNodeTypeName());
                         // redefine node
                        node.onRedefine(ndi.unwrap().getId());
                        node.onRedefine(ndi.unwrap());
                     } catch (ConstraintViolationException cve) {
                         // no suitable definition found for this child node,
                         // remove it
@@ -1451,7 +1459,7 @@ public class NodeImpl extends ItemImpl implements Node {
         }
 
         // Get the applicable child node definition for this node.
        NodeDefinitionImpl def;
        org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl def;
         try {
             def = getApplicableChildNodeDefinition(nodeName, nodeTypeName);
         } catch (RepositoryException e) {
@@ -1493,7 +1501,7 @@ public class NodeImpl extends ItemImpl implements Node {
         session.getValidator().checkModify(this, options, Permission.NONE);
 
         // now do create the child node
        return createChildNode(nodeName, def, nt, id);
        return createChildNode(nodeName, nt, id);
     }
 
     /**
@@ -1890,7 +1898,7 @@ public class NodeImpl extends ItemImpl implements Node {
         session.getValidator().checkModify(this, options, Permission.NONE);
 
         // (4) check for name collisions
        NodeDefinitionImpl def;
        org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl def;
         try {
             def = getApplicableChildNodeDefinition(name, null);
         } catch (RepositoryException re) {
@@ -3677,26 +3685,26 @@ public class NodeImpl extends ItemImpl implements Node {
         }
 
         // get applicable definition for this node using new primary type
        NodeDefId defId;
        QNodeDefinition nodeDef;
         try {
             NodeImpl parent = (NodeImpl) getParent();
            defId = parent.getApplicableChildNodeDefinition(getQName(), ntName).unwrap().getId();
            nodeDef = parent.getApplicableChildNodeDefinition(getQName(), ntName).unwrap();
         } catch (RepositoryException re) {
             String msg = this + ": no applicable definition found in parent node's node type";
             log.debug(msg);
             throw new ConstraintViolationException(msg, re);
         }
 
        if (!defId.equals(state.getDefinitionId())) {
            onRedefine(defId);
        if (!nodeDef.equals(itemMgr.getDefinition(state).unwrap())) {
            onRedefine(nodeDef);
         }
 
        Set<ItemDef> oldDefs = new HashSet<ItemDef>(Arrays.asList(entOld.getAllItemDefs()));
        Set<ItemDef> newDefs = new HashSet<ItemDef>(Arrays.asList(entNew.getAllItemDefs()));
        Set<ItemDef> allDefs = new HashSet<ItemDef>(Arrays.asList(entAll.getAllItemDefs()));
        Set<QItemDefinition> oldDefs = new HashSet<QItemDefinition>(Arrays.asList(entOld.getAllItemDefs()));
        Set<QItemDefinition> newDefs = new HashSet<QItemDefinition>(Arrays.asList(entNew.getAllItemDefs()));
        Set<QItemDefinition> allDefs = new HashSet<QItemDefinition>(Arrays.asList(entAll.getAllItemDefs()));
 
         // added child item definitions
        Set<ItemDef> addedDefs = new HashSet<ItemDef>(newDefs);
        Set<QItemDefinition> addedDefs = new HashSet<QItemDefinition>(newDefs);
         addedDefs.removeAll(oldDefs);
 
         // referential integrity check
@@ -3732,7 +3740,7 @@ public class NodeImpl extends ItemImpl implements Node {
                 PropertyState propState =
                         (PropertyState) stateMgr.getItemState(
                                 new PropertyId(thisState.getNodeId(), propName));
                if (!allDefs.contains(ntReg.getPropDef(propState.getDefinitionId()))) {
                if (!allDefs.contains(itemMgr.getDefinition(propState).unwrap())) {
                     // try to find new applicable definition first and
                     // redefine property if possible
                     try {
@@ -3742,7 +3750,7 @@ public class NodeImpl extends ItemImpl implements Node {
                             removeChildProperty(propName);
                             continue;
                         }
                        PropertyDefinitionImpl pdi = getApplicablePropertyDefinition(
                        org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl pdi = getApplicablePropertyDefinition(
                                 propName, propState.getType(),
                                 propState.isMultiValued(), false);
                         if (pdi.getRequiredType() != PropertyType.UNDEFINED
@@ -3756,7 +3764,7 @@ public class NodeImpl extends ItemImpl implements Node {
                                                 pdi.getRequiredType(),
                                                 session.getValueFactory());
                                 // redefine property
                                prop.onRedefine(pdi.unwrap().getId());
                                prop.onRedefine(pdi.unwrap());
                                 // set converted values
                                 prop.setValue(values);
                             } else {
@@ -3767,13 +3775,13 @@ public class NodeImpl extends ItemImpl implements Node {
                                                 pdi.getRequiredType(),
                                                 session.getValueFactory());
                                 // redefine property
                                prop.onRedefine(pdi.unwrap().getId());
                                prop.onRedefine(pdi.unwrap());
                                 // set converted values
                                 prop.setValue(value);
                             }
                         } else {
                             // redefine property
                            prop.onRedefine(pdi.unwrap().getId());
                            prop.onRedefine(pdi.unwrap());
                         }
                         // update collection of added definitions
                         addedDefs.remove(pdi.unwrap());
@@ -3800,7 +3808,7 @@ public class NodeImpl extends ItemImpl implements Node {
             ChildNodeEntry entry = list.get(i);
             try {
                 NodeState nodeState = (NodeState) stateMgr.getItemState(entry.getId());
                if (!allDefs.contains(ntReg.getNodeDef(nodeState.getDefinitionId()))) {
                if (!allDefs.contains(itemMgr.getDefinition(nodeState).unwrap())) {
                     // try to find new applicable definition first and
                     // redefine node if possible
                     try {
@@ -3810,11 +3818,11 @@ public class NodeImpl extends ItemImpl implements Node {
                             removeChildNode(entry.getName(), entry.getIndex());
                             continue;
                         }
                        NodeDefinitionImpl ndi = getApplicableChildNodeDefinition(
                        org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl ndi = getApplicableChildNodeDefinition(
                                 entry.getName(),
                                 nodeState.getNodeTypeName());
                         // redefine node
                        node.onRedefine(ndi.unwrap().getId());
                        node.onRedefine(ndi.unwrap());
                         // update collection of added definitions
                         addedDefs.remove(ndi.unwrap());
                     } catch (ConstraintViolationException cve) {
@@ -3832,15 +3840,14 @@ public class NodeImpl extends ItemImpl implements Node {
 
         // create items that are defined as auto-created by the new primary node
         // type and at the same time were not present with the old nt
        for (Iterator<ItemDef> iter = addedDefs.iterator(); iter.hasNext();) {
            ItemDef def = iter.next();
        for (QItemDefinition def : addedDefs) {
             if (def.isAutoCreated()) {
                 if (def.definesNode()) {
                    NodeDefinitionImpl ndi = ntMgr.getNodeDefinition(((NodeDef) def).getId());
                    createChildNode(ndi.getQName(), ndi, (NodeTypeImpl) ndi.getDefaultPrimaryType(), null);
                    NodeDefinitionImpl ndi = ntMgr.getNodeDefinition((QNodeDefinition) def);
                    createChildNode(def.getName(), (NodeTypeImpl) ndi.getDefaultPrimaryType(), null);
                 } else {
                    PropertyDefinitionImpl pdi = ntMgr.getPropertyDefinition(((PropDef) def).getId());
                    createChildProperty(pdi.getQName(), pdi.getRequiredType(), pdi);
                    PropertyDefinitionImpl pdi = ntMgr.getPropertyDefinition((QPropertyDefinition) def);
                    createChildProperty(pdi.unwrap().getName(), pdi.getRequiredType(), pdi);
                 }
             }
         }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeTypeInstanceHandler.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeTypeInstanceHandler.java
index 89e27384a..8832a2a55 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeTypeInstanceHandler.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeTypeInstanceHandler.java
@@ -19,10 +19,10 @@ package org.apache.jackrabbit.core;
 import java.util.Calendar;
 import java.util.Set;
 
import org.apache.jackrabbit.core.nodetype.PropDef;
 import org.apache.jackrabbit.core.state.NodeState;
 import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
 
 /**
@@ -60,7 +60,7 @@ public class NodeTypeInstanceHandler {
      * @return the computed values
      */
     public InternalValue[] computeSystemGeneratedPropertyValues(NodeState parent, 
                                                                PropDef def) {
                                                                QPropertyDefinition def) {
 
         InternalValue[] genValues = null;
 
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/PropertyImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/PropertyImpl.java
index b80130ddf..81f0a9fd8 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/PropertyImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/PropertyImpl.java
@@ -38,8 +38,6 @@ import javax.jcr.nodetype.PropertyDefinition;
 import javax.jcr.version.VersionException;
 
 import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.nodetype.PropertyDefinitionImpl;
 import org.apache.jackrabbit.core.security.authorization.Permission;
 import org.apache.jackrabbit.core.state.ItemState;
 import org.apache.jackrabbit.core.state.ItemStateException;
@@ -47,6 +45,7 @@ import org.apache.jackrabbit.core.state.PropertyState;
 import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QPropertyDefinition;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
 import org.apache.jackrabbit.spi.commons.value.ValueFormat;
 import org.apache.jackrabbit.value.ValueHelper;
@@ -137,7 +136,6 @@ public class PropertyImpl extends ItemImpl implements Property {
                 throw new InvalidItemStateException(msg);
             }
             // copy state from transient state
            persistentState.setDefinitionId(transientState.getDefinitionId());
             persistentState.setType(transientState.getType());
             persistentState.setMultiValued(transientState.isMultiValued());
             persistentState.setValues(transientState.getValues());
@@ -162,19 +160,14 @@ public class PropertyImpl extends ItemImpl implements Property {
             stateMgr.disconnectTransientItemState(thisState);
         }
         // reapply transient changes
        thisState.setDefinitionId(transientState.getDefinitionId());
         thisState.setType(transientState.getType());
         thisState.setMultiValued(transientState.isMultiValued());
         thisState.setValues(transientState.getValues());
     }
 
    protected void onRedefine(PropDefId defId) throws RepositoryException {
        PropertyDefinitionImpl newDef =
                session.getNodeTypeManager().getPropertyDefinition(defId);
        // modify the state of 'this', i.e. the target property
        PropertyState thisState = (PropertyState) getOrCreateTransientItemState();
        // set id of new definition
        thisState.setDefinitionId(defId);
    protected void onRedefine(QPropertyDefinition def) throws RepositoryException {
        org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl newDef =
                session.getNodeTypeManager().getPropertyDefinition(def);
         data.setDefinition(newDef);
     }
 
@@ -433,7 +426,6 @@ public class PropertyImpl extends ItemImpl implements Property {
      * @throws RepositoryException
      */
     public InternalValue internalGetValue() throws RepositoryException {
        final PropertyDefinition definition = data.getPropertyDefinition();
         if (isMultiple()) {
             throw new ValueFormatException(
                     this + " is a multi-valued property,"
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ProtectedItemModifier.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ProtectedItemModifier.java
index 0417d2bf8..14223228a 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ProtectedItemModifier.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/ProtectedItemModifier.java
@@ -23,7 +23,6 @@ import javax.jcr.RepositoryException;
 import javax.jcr.Value;
 
 import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeDefinitionImpl;
 import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
 import org.apache.jackrabbit.core.retention.RetentionManagerImpl;
 import org.apache.jackrabbit.core.security.AccessManager;
@@ -70,7 +69,7 @@ public abstract class ProtectedItemModifier {
         parentImpl.checkSetProperty();
 
         NodeTypeImpl nodeType = parentImpl.session.getNodeTypeManager().getNodeType(ntName);
        NodeDefinitionImpl def = parentImpl.getApplicableChildNodeDefinition(name, ntName);
        org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl def = parentImpl.getApplicableChildNodeDefinition(name, ntName);
 
         // check for name collisions
         // TODO: improve. copied from NodeImpl
@@ -90,7 +89,7 @@ public abstract class ProtectedItemModifier {
             }
         }
 
        return parentImpl.createChildNode(name, def, nodeType, null);
        return parentImpl.createChildNode(name, nodeType, null);
     }
 
     protected Property setProperty(NodeImpl parentImpl, Name name, Value value) throws RepositoryException {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/SessionImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/SessionImpl.java
index 867239597..511d502bd 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/SessionImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/SessionImpl.java
@@ -29,7 +29,6 @@ import org.apache.jackrabbit.core.config.WorkspaceConfig;
 import org.apache.jackrabbit.core.data.GarbageCollector;
 import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.lock.LockManager;
import org.apache.jackrabbit.core.nodetype.NodeDefinitionImpl;
 import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
 import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
 import org.apache.jackrabbit.core.persistence.IterablePersistenceManager;
@@ -1081,7 +1080,7 @@ public class SessionImpl extends AbstractSession
         // check constraints
         // get applicable definition of target node at new location
         NodeTypeImpl nt = (NodeTypeImpl) targetNode.getPrimaryNodeType();
        NodeDefinitionImpl newTargetDef;
        org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl newTargetDef;
         try {
             newTargetDef = destParentNode.getApplicableChildNodeDefinition(destName.getName(), nt.getQName());
         } catch (RepositoryException re) {
@@ -1140,7 +1139,7 @@ public class SessionImpl extends AbstractSession
         }
 
         // change definition of target
        targetNode.onRedefine(newTargetDef.unwrap().getId());
        targetNode.onRedefine(newTargetDef.unwrap());
     }
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/lock/LockManagerImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/lock/LockManagerImpl.java
index efbeb9570..bc6a03950 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/lock/LockManagerImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/lock/LockManagerImpl.java
@@ -20,7 +20,6 @@ import EDU.oswego.cs.dl.util.concurrent.ReentrantLock;
 import org.apache.commons.collections.map.LinkedMap;
 import org.apache.commons.io.IOUtils;
 import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.ItemValidator;
 import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.NodeImpl;
 import org.apache.jackrabbit.core.id.PropertyId;
@@ -34,7 +33,6 @@ import org.apache.jackrabbit.core.cluster.LockEventListener;
 import org.apache.jackrabbit.core.fs.FileSystem;
 import org.apache.jackrabbit.core.fs.FileSystemException;
 import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.nodetype.PropDef;
 import org.apache.jackrabbit.core.observation.EventImpl;
 import org.apache.jackrabbit.core.observation.SynchronousEventListener;
 import org.apache.jackrabbit.core.state.ItemStateException;
@@ -874,7 +872,6 @@ public class LockManagerImpl implements LockManager, SynchronousEventListener,
         SessionImpl editingSession = (SessionImpl) node.getSession();
         WorkspaceImpl wsp = (WorkspaceImpl) editingSession.getWorkspace();
         UpdatableItemStateManager stateMgr = wsp.getItemStateManager();
        ItemValidator helper = new ItemValidator(editingSession.getNodeTypeManager().getNodeTypeRegistry(), wsp.getHierarchyManager(), editingSession);
 
         synchronized (stateMgr) {
             if (stateMgr.inEditMode()) {
@@ -888,9 +885,7 @@ public class LockManagerImpl implements LockManager, SynchronousEventListener,
 
                 PropertyState propState;
                 if (!nodeState.hasPropertyName(NameConstants.JCR_LOCKOWNER)) {
                    PropDef def = helper.findApplicablePropertyDefinition(NameConstants.JCR_LOCKOWNER, PropertyType.STRING, false, nodeState);
                     propState = stateMgr.createNew(NameConstants.JCR_LOCKOWNER, nodeId);
                    propState.setDefinitionId(def.getId());
                     propState.setType(PropertyType.STRING);
                     propState.setMultiValued(false);
                 } else {
@@ -901,9 +896,7 @@ public class LockManagerImpl implements LockManager, SynchronousEventListener,
                 stateMgr.store(nodeState);
 
                 if (!nodeState.hasPropertyName(NameConstants.JCR_LOCKISDEEP)) {
                    PropDef def = helper.findApplicablePropertyDefinition(NameConstants.JCR_LOCKISDEEP, PropertyType.BOOLEAN, false, nodeState);
                     propState = stateMgr.createNew(NameConstants.JCR_LOCKISDEEP, nodeId);
                    propState.setDefinitionId(def.getId());
                     propState.setType(PropertyType.BOOLEAN);
                     propState.setMultiValued(false);
                 } else {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/EffectiveNodeType.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/EffectiveNodeType.java
index 98f1fbcb2..cd5c3ee34 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/EffectiveNodeType.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/EffectiveNodeType.java
@@ -19,6 +19,9 @@ package org.apache.jackrabbit.core.nodetype;
 import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -32,6 +35,8 @@ import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.TreeSet;
import java.util.Set;
import java.util.HashSet;
 
 /**
  * An <code>EffectiveNodeType</code> represents one or more
@@ -51,9 +56,9 @@ public class EffectiveNodeType implements Cloneable {
     // (through inheritance) included node types.
     private final TreeSet<Name> allNodeTypes;
     // map of named item definitions (maps name to list of definitions)
    private final HashMap<Name, List<ItemDef>> namedItemDefs;
    private final HashMap<Name, List<QItemDefinition>> namedItemDefs;
     // list of unnamed item definitions (i.e. residual definitions)
    private final ArrayList<ItemDef> unnamedItemDefs;
    private final ArrayList<QItemDefinition> unnamedItemDefs;
 
     // flag indicating whether any included node type supports orderable child nodes
     private boolean orderableChildNodes;
@@ -67,8 +72,8 @@ public class EffectiveNodeType implements Cloneable {
         mergedNodeTypes = new TreeSet<Name>();
         inheritedNodeTypes = new TreeSet<Name>();
         allNodeTypes = new TreeSet<Name>();
        namedItemDefs = new HashMap<Name, List<ItemDef>>();
        unnamedItemDefs = new ArrayList<ItemDef>();
        namedItemDefs = new HashMap<Name, List<QItemDefinition>>();
        unnamedItemDefs = new ArrayList<QItemDefinition>();
         orderableChildNodes = false;
         primaryItemName = null;
     }
@@ -104,13 +109,13 @@ public class EffectiveNodeType implements Cloneable {
         // map of all item definitions (maps id to definition)
         // used to effectively detect ambiguous child definitions where
         // ambiguity is defined in terms of definition identity
        HashMap<Object, ItemDef> itemDefIds = new HashMap<Object, ItemDef>();
        Set<QItemDefinition> itemDefs = new HashSet<QItemDefinition>();
 
        NodeDef[] cnda = ntd.getChildNodeDefs();
        for (NodeDef aCnda : cnda) {
        QNodeDefinition[] cnda = ntd.getChildNodeDefs();
        for (QNodeDefinition aCnda : cnda) {
             // check if child node definition would be ambiguous within
             // this node type definition
            if (itemDefIds.containsKey(aCnda.getId())) {
            if (itemDefs.contains(aCnda)) {
                 // conflict
                 String msg;
                 if (aCnda.definesResidual()) {
@@ -122,7 +127,7 @@ public class EffectiveNodeType implements Cloneable {
                 log.debug(msg);
                 throw new NodeTypeConflictException(msg);
             } else {
                itemDefIds.put(aCnda.getId(), aCnda);
                itemDefs.add(aCnda);
             }
             if (aCnda.definesResidual()) {
                 // residual node definition
@@ -130,9 +135,9 @@ public class EffectiveNodeType implements Cloneable {
             } else {
                 // named node definition
                 Name name = aCnda.getName();
                List<ItemDef> defs = ent.namedItemDefs.get(name);
                List<QItemDefinition> defs = ent.namedItemDefs.get(name);
                 if (defs == null) {
                    defs = new ArrayList<ItemDef>();
                    defs = new ArrayList<QItemDefinition>();
                     ent.namedItemDefs.put(name, defs);
                 }
                 if (defs.size() > 0) {
@@ -140,7 +145,7 @@ public class EffectiveNodeType implements Cloneable {
                      * there already exists at least one definition with that
                      * name; make sure none of them is auto-create
                      */
                    for (ItemDef def : defs) {
                    for (QItemDefinition def : defs) {
                         if (aCnda.isAutoCreated() || def.isAutoCreated()) {
                             // conflict
                             String msg = "There are more than one 'auto-create' item definitions for '"
@@ -153,11 +158,11 @@ public class EffectiveNodeType implements Cloneable {
                 defs.add(aCnda);
             }
         }
        PropDef[] pda = ntd.getPropertyDefs();
        for (PropDef aPda : pda) {
        QPropertyDefinition[] pda = ntd.getPropertyDefs();
        for (QPropertyDefinition aPda : pda) {
             // check if property definition would be ambiguous within
             // this node type definition
            if (itemDefIds.containsKey(aPda.getId())) {
            if (itemDefs.contains(aPda)) {
                 // conflict
                 String msg;
                 if (aPda.definesResidual()) {
@@ -169,7 +174,7 @@ public class EffectiveNodeType implements Cloneable {
                 log.debug(msg);
                 throw new NodeTypeConflictException(msg);
             } else {
                itemDefIds.put(aPda.getId(), aPda);
                itemDefs.add(aPda);
             }
             if (aPda.definesResidual()) {
                 // residual property definition
@@ -177,9 +182,9 @@ public class EffectiveNodeType implements Cloneable {
             } else {
                 // named property definition
                 Name name = aPda.getName();
                List<ItemDef> defs = ent.namedItemDefs.get(name);
                List<QItemDefinition> defs = ent.namedItemDefs.get(name);
                 if (defs == null) {
                    defs = new ArrayList<ItemDef>();
                    defs = new ArrayList<QItemDefinition>();
                     ent.namedItemDefs.put(name, defs);
                 }
                 if (defs.size() > 0) {
@@ -187,7 +192,7 @@ public class EffectiveNodeType implements Cloneable {
                      * there already exists at least one definition with that
                      * name; make sure none of them is auto-create
                      */
                    for (ItemDef def : defs) {
                    for (QItemDefinition def : defs) {
                         if (aPda.isAutoCreated() || def.isAutoCreated()) {
                             // conflict
                             String msg = "There are more than one 'auto-create' item definitions for '"
@@ -276,280 +281,280 @@ public class EffectiveNodeType implements Cloneable {
         return allNodeTypes.toArray(new Name[allNodeTypes.size()]);
     }
 
    public ItemDef[] getAllItemDefs() {
    public QItemDefinition[] getAllItemDefs() {
         if (namedItemDefs.size() == 0 && unnamedItemDefs.size() == 0) {
            return ItemDef.EMPTY_ARRAY;
            return QItemDefinition.EMPTY_ARRAY;
         }
        ArrayList<ItemDef> defs = new ArrayList<ItemDef>(namedItemDefs.size() + unnamedItemDefs.size());
        for (List<ItemDef> itemDefs : namedItemDefs.values()) {
        ArrayList<QItemDefinition> defs = new ArrayList<QItemDefinition>(namedItemDefs.size() + unnamedItemDefs.size());
        for (List<QItemDefinition> itemDefs : namedItemDefs.values()) {
             defs.addAll(itemDefs);
         }
         defs.addAll(unnamedItemDefs);
         if (defs.size() == 0) {
            return ItemDef.EMPTY_ARRAY;
            return QItemDefinition.EMPTY_ARRAY;
         }
        return defs.toArray(new ItemDef[defs.size()]);
        return defs.toArray(new QItemDefinition[defs.size()]);
     }
 
    public ItemDef[] getNamedItemDefs() {
    public QItemDefinition[] getNamedItemDefs() {
         if (namedItemDefs.size() == 0) {
            return ItemDef.EMPTY_ARRAY;
            return QItemDefinition.EMPTY_ARRAY;
         }
        ArrayList<ItemDef> defs = new ArrayList<ItemDef>(namedItemDefs.size());
        for (List<ItemDef> itemDefs : namedItemDefs.values()) {
        ArrayList<QItemDefinition> defs = new ArrayList<QItemDefinition>(namedItemDefs.size());
        for (List<QItemDefinition> itemDefs : namedItemDefs.values()) {
             defs.addAll(itemDefs);
         }
         if (defs.size() == 0) {
            return ItemDef.EMPTY_ARRAY;
            return QItemDefinition.EMPTY_ARRAY;
         }
        return defs.toArray(new ItemDef[defs.size()]);
        return defs.toArray(new QItemDefinition[defs.size()]);
     }
 
    public ItemDef[] getUnnamedItemDefs() {
    public QItemDefinition[] getUnnamedItemDefs() {
         if (unnamedItemDefs.size() == 0) {
            return ItemDef.EMPTY_ARRAY;
            return QItemDefinition.EMPTY_ARRAY;
         }
        return unnamedItemDefs.toArray(new ItemDef[unnamedItemDefs.size()]);
        return unnamedItemDefs.toArray(new QItemDefinition[unnamedItemDefs.size()]);
     }
 
     public boolean hasNamedItemDef(Name name) {
         return namedItemDefs.containsKey(name);
     }
 
    public ItemDef[] getNamedItemDefs(Name name) {
        List<ItemDef> defs = namedItemDefs.get(name);
    public QItemDefinition[] getNamedItemDefs(Name name) {
        List<QItemDefinition> defs = namedItemDefs.get(name);
         if (defs == null || defs.size() == 0) {
            return ItemDef.EMPTY_ARRAY;
            return QItemDefinition.EMPTY_ARRAY;
         }
        return defs.toArray(new ItemDef[defs.size()]);
        return defs.toArray(new QItemDefinition[defs.size()]);
     }
 
    public NodeDef[] getAllNodeDefs() {
    public QNodeDefinition[] getAllNodeDefs() {
         if (namedItemDefs.size() == 0 && unnamedItemDefs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
            return QNodeDefinition.EMPTY_ARRAY;
         }
        ArrayList<NodeDef> defs = new ArrayList<NodeDef>(namedItemDefs.size() + unnamedItemDefs.size());
        for (ItemDef def : unnamedItemDefs) {
        ArrayList<QNodeDefinition> defs = new ArrayList<QNodeDefinition>(namedItemDefs.size() + unnamedItemDefs.size());
        for (QItemDefinition def : unnamedItemDefs) {
             if (def.definesNode()) {
                defs.add((NodeDef) def);
                defs.add((QNodeDefinition) def);
             }
         }
        for (List<ItemDef> list: namedItemDefs.values()) {
            for (ItemDef def : list) {
        for (List<QItemDefinition> list: namedItemDefs.values()) {
            for (QItemDefinition def : list) {
                 if (def.definesNode()) {
                    defs.add((NodeDef) def);
                    defs.add((QNodeDefinition) def);
                 }
             }
         }
         if (defs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
            return QNodeDefinition.EMPTY_ARRAY;
         }
        return defs.toArray(new NodeDef[defs.size()]);
        return defs.toArray(new QNodeDefinition[defs.size()]);
     }
 
    public NodeDef[] getNamedNodeDefs() {
    public QItemDefinition[] getNamedNodeDefs() {
         if (namedItemDefs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
            return QNodeDefinition.EMPTY_ARRAY;
         }
        ArrayList<NodeDef> defs = new ArrayList<NodeDef>(namedItemDefs.size());
        for (List<ItemDef> list : namedItemDefs.values()) {
            for (ItemDef def : list) {
        ArrayList<QNodeDefinition> defs = new ArrayList<QNodeDefinition>(namedItemDefs.size());
        for (List<QItemDefinition> list : namedItemDefs.values()) {
            for (QItemDefinition def : list) {
                 if (def.definesNode()) {
                    defs.add((NodeDef) def);
                    defs.add((QNodeDefinition) def);
                 }
             }
         }
         if (defs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
            return QNodeDefinition.EMPTY_ARRAY;
         }
        return defs.toArray(new NodeDef[defs.size()]);
        return defs.toArray(new QNodeDefinition[defs.size()]);
     }
 
    public NodeDef[] getNamedNodeDefs(Name name) {
        List<ItemDef> list = namedItemDefs.get(name);
    public QItemDefinition[] getNamedNodeDefs(Name name) {
        List<QItemDefinition> list = namedItemDefs.get(name);
         if (list == null || list.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
            return QNodeDefinition.EMPTY_ARRAY;
         }
        ArrayList<NodeDef> defs = new ArrayList<NodeDef>(list.size());
        for (ItemDef def : list) {
        ArrayList<QNodeDefinition> defs = new ArrayList<QNodeDefinition>(list.size());
        for (QItemDefinition def : list) {
             if (def.definesNode()) {
                defs.add((NodeDef) def);
                defs.add((QNodeDefinition) def);
             }
         }
         if (defs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
            return QNodeDefinition.EMPTY_ARRAY;
         }
        return defs.toArray(new NodeDef[defs.size()]);
        return defs.toArray(new QNodeDefinition[defs.size()]);
     }
 
    public NodeDef[] getUnnamedNodeDefs() {
    public QNodeDefinition[] getUnnamedNodeDefs() {
         if (unnamedItemDefs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
            return QNodeDefinition.EMPTY_ARRAY;
         }
        ArrayList<NodeDef> defs = new ArrayList<NodeDef>(unnamedItemDefs.size());
        for (ItemDef def : unnamedItemDefs) {
        ArrayList<QNodeDefinition> defs = new ArrayList<QNodeDefinition>(unnamedItemDefs.size());
        for (QItemDefinition def : unnamedItemDefs) {
             if (def.definesNode()) {
                defs.add((NodeDef) def);
                defs.add((QNodeDefinition) def);
             }
         }
         if (defs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
            return QNodeDefinition.EMPTY_ARRAY;
         }
        return defs.toArray(new NodeDef[defs.size()]);
        return defs.toArray(new QNodeDefinition[defs.size()]);
     }
 
    public NodeDef[] getAutoCreateNodeDefs() {
    public QNodeDefinition[] getAutoCreateNodeDefs() {
         // since auto-create items must have a name,
         // we're only searching the named item definitions
         if (namedItemDefs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
            return QNodeDefinition.EMPTY_ARRAY;
         }
        ArrayList<NodeDef> defs = new ArrayList<NodeDef>(namedItemDefs.size());
        for (List<ItemDef> list : namedItemDefs.values()) {
            for (ItemDef def : list) {
        ArrayList<QNodeDefinition> defs = new ArrayList<QNodeDefinition>(namedItemDefs.size());
        for (List<QItemDefinition> list : namedItemDefs.values()) {
            for (QItemDefinition def : list) {
                 if (def.definesNode() && def.isAutoCreated()) {
                    defs.add((NodeDef) def);
                    defs.add((QNodeDefinition) def);
                 }
             }
         }
         if (defs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
            return QNodeDefinition.EMPTY_ARRAY;
         }
        return defs.toArray(new NodeDef[defs.size()]);
        return defs.toArray(new QNodeDefinition[defs.size()]);
     }
 
    public PropDef[] getAllPropDefs() {
    public QPropertyDefinition[] getAllPropDefs() {
         if (namedItemDefs.size() == 0 && unnamedItemDefs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
            return QPropertyDefinition.EMPTY_ARRAY;
         }
        ArrayList<PropDef> defs = new ArrayList<PropDef>(namedItemDefs.size() + unnamedItemDefs.size());
        for (ItemDef def : unnamedItemDefs) {
        ArrayList<QPropertyDefinition> defs = new ArrayList<QPropertyDefinition>(namedItemDefs.size() + unnamedItemDefs.size());
        for (QItemDefinition def : unnamedItemDefs) {
             if (!def.definesNode()) {
                defs.add((PropDef) def);
                defs.add((QPropertyDefinition) def);
             }
         }
        for (List<ItemDef> list: namedItemDefs.values()) {
            for (ItemDef def : list) {
        for (List<QItemDefinition> list: namedItemDefs.values()) {
            for (QItemDefinition def : list) {
                 if (!def.definesNode()) {
                    defs.add((PropDef) def);
                    defs.add((QPropertyDefinition) def);
                 }
             }
         }
         if (defs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
            return QPropertyDefinition.EMPTY_ARRAY;
         }
        return defs.toArray(new PropDef[defs.size()]);
        return defs.toArray(new QPropertyDefinition[defs.size()]);
     }
 
    public PropDef[] getNamedPropDefs() {
    public QPropertyDefinition[] getNamedPropDefs() {
         if (namedItemDefs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
            return QPropertyDefinition.EMPTY_ARRAY;
         }
        ArrayList<PropDef> defs = new ArrayList<PropDef>(namedItemDefs.size());
        for (List<ItemDef> list : namedItemDefs.values()) {
            for (ItemDef def : list) {
        ArrayList<QPropertyDefinition> defs = new ArrayList<QPropertyDefinition>(namedItemDefs.size());
        for (List<QItemDefinition> list : namedItemDefs.values()) {
            for (QItemDefinition def : list) {
                 if (!def.definesNode()) {
                    defs.add((PropDef) def);
                    defs.add((QPropertyDefinition) def);
                 }
             }
         }
         if (defs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
            return QPropertyDefinition.EMPTY_ARRAY;
         }
        return defs.toArray(new PropDef[defs.size()]);
        return defs.toArray(new QPropertyDefinition[defs.size()]);
     }
 
    public PropDef[] getNamedPropDefs(Name name) {
        List<ItemDef> list = namedItemDefs.get(name);
    public QPropertyDefinition[] getNamedPropDefs(Name name) {
        List<QItemDefinition> list = namedItemDefs.get(name);
         if (list == null || list.size() == 0) {
            return PropDef.EMPTY_ARRAY;
            return QPropertyDefinition.EMPTY_ARRAY;
         }
        ArrayList<PropDef> defs = new ArrayList<PropDef>(list.size());
        for (ItemDef def : list) {
        ArrayList<QPropertyDefinition> defs = new ArrayList<QPropertyDefinition>(list.size());
        for (QItemDefinition def : list) {
             if (!def.definesNode()) {
                defs.add((PropDef) def);
                defs.add((QPropertyDefinition) def);
             }
         }
         if (defs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
            return QPropertyDefinition.EMPTY_ARRAY;
         }
        return defs.toArray(new PropDef[defs.size()]);
        return defs.toArray(new QPropertyDefinition[defs.size()]);
     }
 
    public PropDef[] getUnnamedPropDefs() {
    public QPropertyDefinition[] getUnnamedPropDefs() {
         if (unnamedItemDefs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
            return QPropertyDefinition.EMPTY_ARRAY;
         }
        ArrayList<PropDef> defs = new ArrayList<PropDef>(unnamedItemDefs.size());
        for (ItemDef def : unnamedItemDefs) {
        ArrayList<QPropertyDefinition> defs = new ArrayList<QPropertyDefinition>(unnamedItemDefs.size());
        for (QItemDefinition def : unnamedItemDefs) {
             if (!def.definesNode()) {
                defs.add((PropDef) def);
                defs.add((QPropertyDefinition) def);
             }
         }
         if (defs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
            return QPropertyDefinition.EMPTY_ARRAY;
         }
        return defs.toArray(new PropDef[defs.size()]);
        return defs.toArray(new QPropertyDefinition[defs.size()]);
     }
 
    public PropDef[] getAutoCreatePropDefs() {
    public QPropertyDefinition[] getAutoCreatePropDefs() {
         // since auto-create items must have a name,
         // we're only searching the named item definitions
         if (namedItemDefs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
            return QPropertyDefinition.EMPTY_ARRAY;
         }
        ArrayList<PropDef> defs = new ArrayList<PropDef>(namedItemDefs.size());
        for (List<ItemDef> list : namedItemDefs.values()) {
            for (ItemDef def : list) {
        ArrayList<QPropertyDefinition> defs = new ArrayList<QPropertyDefinition>(namedItemDefs.size());
        for (List<QItemDefinition> list : namedItemDefs.values()) {
            for (QItemDefinition def : list) {
                 if (!def.definesNode() && def.isAutoCreated()) {
                    defs.add((PropDef) def);
                    defs.add((QPropertyDefinition) def);
                 }
             }
         }
         if (defs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
            return QPropertyDefinition.EMPTY_ARRAY;
         }
        return defs.toArray(new PropDef[defs.size()]);
        return defs.toArray(new QPropertyDefinition[defs.size()]);
     }
 
    public PropDef[] getMandatoryPropDefs() {
    public QPropertyDefinition[] getMandatoryPropDefs() {
         // since mandatory items must have a name,
         // we're only searching the named item definitions
         if (namedItemDefs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
            return QPropertyDefinition.EMPTY_ARRAY;
         }
        ArrayList<PropDef> defs = new ArrayList<PropDef>(namedItemDefs.size());
        for (List<ItemDef> list : namedItemDefs.values()) {
            for (ItemDef def : list) {
        ArrayList<QPropertyDefinition> defs = new ArrayList<QPropertyDefinition>(namedItemDefs.size());
        for (List<QItemDefinition> list : namedItemDefs.values()) {
            for (QItemDefinition def : list) {
                 if (!def.definesNode() && def.isMandatory()) {
                    defs.add((PropDef) def);
                    defs.add((QPropertyDefinition) def);
                 }
             }
         }
         if (defs.size() == 0) {
            return PropDef.EMPTY_ARRAY;
            return QPropertyDefinition.EMPTY_ARRAY;
         }
        return defs.toArray(new PropDef[defs.size()]);
        return defs.toArray(new QPropertyDefinition[defs.size()]);
     }
 
    public NodeDef[] getMandatoryNodeDefs() {
    public QNodeDefinition[] getMandatoryNodeDefs() {
         // since mandatory items must have a name,
         // we're only searching the named item definitions
         if (namedItemDefs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
            return QNodeDefinition.EMPTY_ARRAY;
         }
        ArrayList<NodeDef> defs = new ArrayList<NodeDef>(namedItemDefs.size());
        for (List<ItemDef> list : namedItemDefs.values()) {
            for (ItemDef def : list) {
        ArrayList<QNodeDefinition> defs = new ArrayList<QNodeDefinition>(namedItemDefs.size());
        for (List<QItemDefinition> list : namedItemDefs.values()) {
            for (QItemDefinition def : list) {
                 if (def.definesNode() && def.isMandatory()) {
                    defs.add((NodeDef) def);
                    defs.add((QNodeDefinition) def);
                 }
             }
         }
         if (defs.size() == 0) {
            return NodeDef.EMPTY_ARRAY;
            return QNodeDefinition.EMPTY_ARRAY;
         }
        return defs.toArray(new NodeDef[defs.size()]);
        return defs.toArray(new QNodeDefinition[defs.size()]);
     }
 
     /**
@@ -591,7 +596,7 @@ public class EffectiveNodeType implements Cloneable {
      *                                      by the the specified values
      * @throws RepositoryException          if another error occurs
      */
    public static void checkSetPropertyValueConstraints(PropDef pd,
    public static void checkSetPropertyValueConstraints(QPropertyDefinition pd,
                                                         InternalValue[] values)
             throws ConstraintViolationException, RepositoryException {
         // check multi-value flag
@@ -661,7 +666,7 @@ public class EffectiveNodeType implements Cloneable {
                 throw new ConstraintViolationException(nodeTypeName + " is mixin.");
             }
         }
        NodeDef nd = getApplicableChildNodeDef(name, nodeTypeName, ntReg);
        QItemDefinition nd = getApplicableChildNodeDef(name, nodeTypeName, ntReg);
         if (nd.isProtected()) {
             throw new ConstraintViolationException(name + " is protected");
         }
@@ -683,7 +688,7 @@ public class EffectiveNodeType implements Cloneable {
      * @throws ConstraintViolationException if no applicable child node definition
      *                                      could be found
      */
    public NodeDef getApplicableChildNodeDef(Name name, Name nodeTypeName,
    public QNodeDefinition getApplicableChildNodeDef(Name name, Name nodeTypeName,
                                              NodeTypeRegistry ntReg)
             throws NoSuchNodeTypeException, ConstraintViolationException {
         EffectiveNodeType entTarget;
@@ -694,10 +699,10 @@ public class EffectiveNodeType implements Cloneable {
         }
 
         // try named node definitions first
        ItemDef[] defs = getNamedItemDefs(name);
        for (ItemDef def : defs) {
        QItemDefinition[] defs = getNamedItemDefs(name);
        for (QItemDefinition def : defs) {
             if (def.definesNode()) {
                NodeDef nd = (NodeDef) def;
                QNodeDefinition nd = (QNodeDefinition) def;
                 Name[] types = nd.getRequiredPrimaryTypes();
                 // node definition with that name exists
                 if (entTarget != null && types != null) {
@@ -715,8 +720,8 @@ public class EffectiveNodeType implements Cloneable {
 
         // no item with that name defined;
         // try residual node definitions
        NodeDef[] nda = getUnnamedNodeDefs();
        for (NodeDef nd : nda) {
        QNodeDefinition[] nda = getUnnamedNodeDefs();
        for (QNodeDefinition nd : nda) {
             if (entTarget != null && nd.getRequiredPrimaryTypes() != null) {
                 // check 'required primary types' constraint
                 if (!entTarget.includesNodeTypes(nd.getRequiredPrimaryTypes())) {
@@ -755,11 +760,11 @@ public class EffectiveNodeType implements Cloneable {
      * @throws ConstraintViolationException if no applicable property definition
      *                                      could be found
      */
    public PropDef getApplicablePropertyDef(Name name, int type,
    public QPropertyDefinition getApplicablePropertyDef(Name name, int type,
                                             boolean multiValued)
             throws ConstraintViolationException {
         // try named property definitions first
        PropDef match =
        QPropertyDefinition match =
                 getMatchingPropDef(getNamedPropDefs(name), type, multiValued);
         if (match != null) {
             return match;
@@ -797,10 +802,10 @@ public class EffectiveNodeType implements Cloneable {
      * @throws ConstraintViolationException if no applicable property definition
      *                                      could be found
      */
    public PropDef getApplicablePropertyDef(Name name, int type)
    public QPropertyDefinition getApplicablePropertyDef(Name name, int type)
             throws ConstraintViolationException {
         // try named property definitions first
        PropDef match = getMatchingPropDef(getNamedPropDefs(name), type);
        QPropertyDefinition match = getMatchingPropDef(getNamedPropDefs(name), type);
         if (match != null) {
             return match;
         }
@@ -816,9 +821,9 @@ public class EffectiveNodeType implements Cloneable {
         throw new ConstraintViolationException("no matching property definition found for " + name);
     }
 
    private PropDef getMatchingPropDef(PropDef[] defs, int type) {
        PropDef match = null;
        for (PropDef pd : defs) {
    private QPropertyDefinition getMatchingPropDef(QPropertyDefinition[] defs, int type) {
        QPropertyDefinition match = null;
        for (QPropertyDefinition pd : defs) {
             int reqType = pd.getRequiredType();
             // match type
             if (reqType == PropertyType.UNDEFINED
@@ -851,10 +856,10 @@ public class EffectiveNodeType implements Cloneable {
         return match;
     }
 
    private PropDef getMatchingPropDef(PropDef[] defs, int type,
    private QPropertyDefinition getMatchingPropDef(QPropertyDefinition[] defs, int type,
                                        boolean multiValued) {
        PropDef match = null;
        for (PropDef pd : defs) {
        QPropertyDefinition match = null;
        for (QPropertyDefinition pd : defs) {
             int reqType = pd.getRequiredType();
             // match type
             if (reqType == PropertyType.UNDEFINED
@@ -886,9 +891,9 @@ public class EffectiveNodeType implements Cloneable {
          * as there might be multiple definitions with the same name and we
          * don't know which one is applicable, we check all of them
          */
        ItemDef[] defs = getNamedItemDefs(name);
        QItemDefinition[] defs = getNamedItemDefs(name);
         if (defs != null) {
            for (ItemDef def : defs) {
            for (QItemDefinition def : defs) {
                 if (def.isMandatory()) {
                     throw new ConstraintViolationException("can't remove mandatory item");
                 }
@@ -908,9 +913,9 @@ public class EffectiveNodeType implements Cloneable {
          * as there might be multiple definitions with the same name and we
          * don't know which one is applicable, we check all of them
          */
        ItemDef[] defs = getNamedNodeDefs(name);
        QItemDefinition[] defs = getNamedNodeDefs(name);
         if (defs != null) {
            for (ItemDef def : defs) {
            for (QItemDefinition def : defs) {
                 if (def.isMandatory()) {
                     throw new ConstraintViolationException("can't remove mandatory node");
                 }
@@ -930,9 +935,9 @@ public class EffectiveNodeType implements Cloneable {
          * as there might be multiple definitions with the same name and we
          * don't know which one is applicable, we check all of them
          */
        ItemDef[] defs = getNamedPropDefs(name);
        QItemDefinition[] defs = getNamedPropDefs(name);
         if (defs != null) {
            for (ItemDef def : defs) {
            for (QItemDefinition def : defs) {
                 if (def.isMandatory()) {
                     throw new ConstraintViolationException("can't remove mandatory property");
                 }
@@ -992,18 +997,18 @@ public class EffectiveNodeType implements Cloneable {
         }
 
         // named item definitions
        ItemDef[] defs = other.getNamedItemDefs();
        for (ItemDef def : defs) {
        QItemDefinition[] defs = other.getNamedItemDefs();
        for (QItemDefinition def : defs) {
             if (includesNodeType(def.getDeclaringNodeType())) {
                 // ignore redundant definitions
                 continue;
             }
             Name name = def.getName();
            List<ItemDef> existingDefs = namedItemDefs.get(name);
            List<QItemDefinition> existingDefs = namedItemDefs.get(name);
             if (existingDefs != null) {
                 if (existingDefs.size() > 0) {
                     // there already exists at least one definition with that name
                    for (ItemDef existingDef : existingDefs) {
                    for (QItemDefinition existingDef : existingDefs) {
                         // make sure none of them is auto-create
                         if (def.isAutoCreated() || existingDef.isAutoCreated()) {
                             // conflict
@@ -1020,8 +1025,8 @@ public class EffectiveNodeType implements Cloneable {
                         if (def.definesNode() == existingDef.definesNode()) {
                             if (!def.definesNode()) {
                                 // property definition
                                PropDef pd = (PropDef) def;
                                PropDef epd = (PropDef) existingDef;
                                QPropertyDefinition pd = (QPropertyDefinition) def;
                                QPropertyDefinition epd = (QPropertyDefinition) existingDef;
                                 // compare type & multiValued flag
                                 if (pd.getRequiredType() == epd.getRequiredType()
                                         && pd.isMultiple() == epd.isMultiple()) {
@@ -1051,7 +1056,7 @@ public class EffectiveNodeType implements Cloneable {
                     }
                 }
             } else {
                existingDefs = new ArrayList<ItemDef>();
                existingDefs = new ArrayList<QItemDefinition>();
                 namedItemDefs.put(name, existingDefs);
             }
             existingDefs.add(def);
@@ -1059,18 +1064,18 @@ public class EffectiveNodeType implements Cloneable {
 
         // residual item definitions
         defs = other.getUnnamedItemDefs();
        for (ItemDef def : defs) {
        for (QItemDefinition def : defs) {
             if (includesNodeType(def.getDeclaringNodeType())) {
                 // ignore redundant definitions
                 continue;
             }
            for (ItemDef existing : unnamedItemDefs) {
            for (QItemDefinition existing : unnamedItemDefs) {
                 // compare with existing definition
                 if (def.definesNode() == existing.definesNode()) {
                     if (!def.definesNode()) {
                         // property definition
                        PropDef pd = (PropDef) def;
                        PropDef epd = (PropDef) existing;
                        QPropertyDefinition pd = (QPropertyDefinition) def;
                        QPropertyDefinition epd = (QPropertyDefinition) existing;
                         // compare type & multiValued flag
                         if (pd.getRequiredType() == epd.getRequiredType()
                                 && pd.isMultiple() == epd.isMultiple()) {
@@ -1085,8 +1090,8 @@ public class EffectiveNodeType implements Cloneable {
                         }
                     } else {
                         // child node definition
                        NodeDef nd = (NodeDef) def;
                        NodeDef end = (NodeDef) existing;
                        QNodeDefinition nd = (QNodeDefinition) def;
                        QNodeDefinition end = (QNodeDefinition) existing;
                         // compare required & default primary types
                         if (Arrays.equals(nd.getRequiredPrimaryTypes(), end.getRequiredPrimaryTypes())
                                 && (nd.getDefaultPrimaryType() == null
@@ -1146,8 +1151,8 @@ public class EffectiveNodeType implements Cloneable {
         clone.inheritedNodeTypes.addAll(inheritedNodeTypes);
         clone.allNodeTypes.addAll(allNodeTypes);
         for (Name name : namedItemDefs.keySet()) {
            List<ItemDef> list = namedItemDefs.get(name);
            clone.namedItemDefs.put(name, new ArrayList<ItemDef>(list));
            List<QItemDefinition> list = namedItemDefs.get(name);
            clone.namedItemDefs.put(name, new ArrayList<QItemDefinition>(list));
         }
         clone.unnamedItemDefs.addAll(unnamedItemDefs);
         clone.orderableChildNodes = orderableChildNodes;
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/ItemDef.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/ItemDef.java
deleted file mode 100644
index 994df7088..000000000
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/ItemDef.java
++ /dev/null
@@ -1,97 +0,0 @@
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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * <code>ItemDef</code> is the internal representation of
 * an item definition. It refers to <code>Name</code>s only
 * and is thus isolated from session-specific namespace mappings.
 *
 * @see javax.jcr.nodetype.ItemDefinition
 */
public interface ItemDef {

    ItemDef[] EMPTY_ARRAY = new ItemDef[0];

    /**
     * The special wildcard name used as the name of residual item definitions.
     */
    Name ANY_NAME = NameConstants.ANY_NAME;

    /**
     * Gets the name of the child item.
     *
     * @return the name of the child item.
     */
    Name getName();

    /**
     * Gets the name of the declaring node type.
     *
     * @return the name of the declaring node type.
     */
    Name getDeclaringNodeType();

    /**
     * Determines whether the item is 'autoCreated'.
     *
     * @return the 'autoCreated' flag.
     */
    boolean isAutoCreated();

    /**
     * Gets the 'onParentVersion' attribute of the item.
     *
     * @return the 'onParentVersion' attribute.
     */
    int getOnParentVersion();

    /**
     * Determines whether the item is 'protected'.
     *
     * @return the 'protected' flag.
     */
    boolean isProtected();

    /**
     * Determines whether the item is 'mandatory'.
     *
     * @return the 'mandatory' flag.
     */
    boolean isMandatory();

    /**
     * Determines whether this item definition defines a residual set of
     * child items. This is equivalent to calling
     * <code>getName().equals(ANY_NAME)</code>.
     *
     * @return <code>true</code> if this definition defines a residual set;
     *         <code>false</code> otherwise.
     */
    boolean definesResidual();

    /**
     * Determines whether this item definition defines a node.
     *
     * @return <code>true</code> if this is a node definition;
     *         <code>false</code> otherwise (i.e. it is a property definition).
     */
    boolean definesNode();
}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/ItemDefImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/ItemDefImpl.java
deleted file mode 100644
index 14bc3c8f1..000000000
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/ItemDefImpl.java
++ /dev/null
@@ -1,235 +0,0 @@
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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QItemDefinition;

import javax.jcr.version.OnParentVersionAction;

/**
 * This abstract class implements the <code>ItemDef</code>
 * interface and additionally provides setter methods for the
 * various item definition attributes.
 */
public abstract class ItemDefImpl implements ItemDef {

    /**
     * The name of the child item.
     */
    private Name name = ItemDef.ANY_NAME;

    /**
     * The name of the declaring node type.
     */
    protected Name declaringNodeType = null;

    /**
     * The 'autoCreated' flag.
     */
    private boolean autoCreated = false;

    /**
     * The 'onParentVersion' attribute.
     */
    private int onParentVersion = OnParentVersionAction.COPY;

    /**
     * The 'protected' flag.
     */
    private boolean writeProtected = false;

    /**
     * The 'mandatory' flag.
     */
    private boolean mandatory = false;

    /**
     * Default constructor.
     */
    public ItemDefImpl() {
    }

    public ItemDefImpl(QItemDefinition def) {
        name = def.getName();
        declaringNodeType = def.getDeclaringNodeType();
        autoCreated = def.isAutoCreated();
        onParentVersion = def.getOnParentVersion();
        writeProtected = def.isProtected();
        mandatory = def.isMandatory();
    }
    
    /**
     * Sets the name of declaring node type.
     *
     * @param declaringNodeType name of the declaring node type (must not be
     *                          <code>null</code>)
     */
    public void setDeclaringNodeType(Name declaringNodeType) {
        if (declaringNodeType == null) {
            throw new IllegalArgumentException("declaringNodeType can not be null");
        }
        this.declaringNodeType = declaringNodeType;
    }

    /**
     * Sets the name of the child item.
     *
     * @param name name of child item (must not be  <code>null</code>)
     */
    public void setName(Name name) {
        if (name == null) {
            throw new IllegalArgumentException("name can not be null");
        }
        this.name = name;
    }

    /**
     * Sets the 'autoCreated' flag.
     *
     * @param autoCreated a <code>boolean</code>
     */
    public void setAutoCreated(boolean autoCreated) {
        this.autoCreated = autoCreated;
    }

    /**
     * Sets the 'onParentVersion' attribute.
     *
     * @param onParentVersion any of the following constants:
     * <UL>
     *    <LI><code>OnParentVersionAction.COPY</code>
     *    <LI><code>OnParentVersionAction.VERSION</code>
     *    <LI><code>OnParentVersionAction.INITIALIZE</code>
     *    <LI><code>OnParentVersionAction.COMPUTE</code>
     *    <LI><code>OnParentVersionAction.IGNORE</code>
     *    <LI><code>OnParentVersionAction.ABORT</code>
     * </UL>
     */
    public void setOnParentVersion(int onParentVersion) {
        this.onParentVersion = onParentVersion;
    }

    /**
     * Sets the 'protected' flag.
     *
     * @param writeProtected a <code>boolean</code>
     */
    public void setProtected(boolean writeProtected) {
        this.writeProtected = writeProtected;
    }

    /**
     * Sets the 'mandatory' flag.
     *
     * @param mandatory a <code>boolean</code>
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    //--------------------------------------------------------------< ItemDef >
    /**
     * {@inheritDoc}
     */
    public Name getDeclaringNodeType() {
        return declaringNodeType;
    }

    /**
     * {@inheritDoc}
     */
    public Name getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAutoCreated() {
        return autoCreated;
    }

    /**
     * {@inheritDoc}
     */
    public int getOnParentVersion() {
        return onParentVersion;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isProtected() {
        return writeProtected;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * {@inheritDoc}
     */
    public boolean definesResidual() {
        return name.equals(ItemDef.ANY_NAME);
    }

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * Compares two item definitions for equality. Returns <code>true</code>
     * if the given object is an item defintion and has the same attributes
     * as this item definition.
     *
     * @param obj the object to compare this item definition with
     * @return <code>true</code> if the object is equal to this item definition,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ItemDefImpl) {
            ItemDefImpl other = (ItemDefImpl) obj;
            return (declaringNodeType == null
                    ? other.declaringNodeType == null
                    : declaringNodeType.equals(other.declaringNodeType))
                    && (name == null ? other.name == null : name.equals(other.name))
                    && autoCreated == other.autoCreated
                    && onParentVersion == other.onParentVersion
                    && writeProtected == other.writeProtected
                    && mandatory == other.mandatory;
        }
        return false;
    }

    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }

}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/ItemDefinitionImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/ItemDefinitionImpl.java
deleted file mode 100644
index edf3332ec..000000000
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/ItemDefinitionImpl.java
++ /dev/null
@@ -1,181 +0,0 @@
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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.NamespaceException;

/**
 * This class implements the <code>ItemDefinition</code> interface.
 * All method calls are delegated to the wrapped {@link ItemDef},
 * performing the translation from <code>Name</code>s to JCR names
 * (and vice versa) where necessary.
 */
abstract class ItemDefinitionImpl implements ItemDefinition {

    /**
     * Logger instance for this class
     */
    private static Logger log = LoggerFactory.getLogger(ItemDefinitionImpl.class);

    /**
     * Literal for 'any name'.
     */
    protected static final String ANY_NAME = "*";

    /**
     * The node type manager of this session.
     */
    protected final NodeTypeManagerImpl ntMgr;

    /**
     * The name/path resolver used to translate <code>Name</code>s to JCR name
     * strings.
     */
    protected final NamePathResolver resolver;

    /**
     * The wrapped item definition.
     */
    protected final ItemDef itemDef;

    /**
     * Package private constructor
     *
     * @param itemDef    item definition
     * @param ntMgr      node type manager
     * @param resolver   name resolver
     */
    ItemDefinitionImpl(ItemDef itemDef, NodeTypeManagerImpl ntMgr,
                       NamePathResolver resolver) {
        this.itemDef = itemDef;
        this.ntMgr = ntMgr;
        this.resolver = resolver;
    }

    /**
     * Checks whether this is a residual item definition.
     *
     * @return <code>true</code> if this is a residual item definition
     */
    public boolean definesResidual() {
        return itemDef.definesResidual();
    }

    /**
     * Gets the <code>Name</code> of the child item. It is an error to
     * call this method if this is a residual item definition.
     *
     * @return the <code>Name</code> of the child item.
     * @see #getName()
     */
    public Name getQName() {
        return itemDef.getName();
    }

    //-------------------------------------------------------< ItemDefinition >
    /**
     * {@inheritDoc}
     */
    public NodeType getDeclaringNodeType() {
        if (ntMgr == null) {
            return null;
        }
        try {
            return ntMgr.getNodeType(itemDef.getDeclaringNodeType());
        } catch (NoSuchNodeTypeException e) {
            // should never get here
            log.error("declaring node type does not exist", e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        if (itemDef.definesResidual()) {
            return ANY_NAME;
        } else {
            try {
                return resolver.getJCRName(itemDef.getName());
            } catch (NamespaceException e) {
                // should never get here
                log.error("encountered unregistered namespace in item name",
                        e);
                // not correct, but an acceptable fallback
                return itemDef.getName().toString();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getOnParentVersion() {
        return itemDef.getOnParentVersion();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAutoCreated() {
        return itemDef.isAutoCreated();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMandatory() {
        return itemDef.isMandatory();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isProtected() {
        return itemDef.isProtected();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemDefinitionImpl)) {
            return false;
        }
        return itemDef.equals(((ItemDefinitionImpl) o).itemDef);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return itemDef.hashCode();
    }
}

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeDef.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeDef.java
deleted file mode 100644
index e19c2c54a..000000000
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeDef.java
++ /dev/null
@@ -1,59 +0,0 @@
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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.spi.Name;

/**
 * <code>NodeDef</code> is the internal representation of
 * a node definition. It refers to <code>Name</code>s only
 * and is thus isolated from session-specific namespace mappings.
 *
 * @see javax.jcr.nodetype.NodeDefinition
 */
public interface NodeDef extends ItemDef {

    NodeDef[] EMPTY_ARRAY = new NodeDef[0];

    /**
     * Returns an identifier for this node definition.
     *
     * @return an identifier for this node definition.
     */
    NodeDefId getId();

    /**
     * Returns the name of the default primary type.
     *
     * @return the name of the default primary type.
     */
    Name getDefaultPrimaryType();

    /**
     * Returns the array of names of the required primary types.
     *
     * @return the array of names of the required primary types.
     */
    Name[] getRequiredPrimaryTypes();

    /**
     * Reports whether this node can have same-name siblings.
     *
     * @return the 'allowsSameNameSiblings' flag.
     */
    boolean allowsSameNameSiblings();
}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeDefId.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeDefId.java
index bb0bac200..ac947b724 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeDefId.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeDefId.java
@@ -17,6 +17,7 @@
 package org.apache.jackrabbit.core.nodetype;
 
 import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
 
 import java.io.Serializable;
 import java.util.Arrays;
@@ -25,7 +26,7 @@ import java.util.Arrays;
  * <code>NodeDefId</code> uniquely identifies a <code>NodeDef</code> in the
  * node type registry.
  */
public class NodeDefId implements Serializable {
class NodeDefId implements Serializable {
 
     /**
      * Serialization UID of this class.
@@ -45,7 +46,7 @@ public class NodeDefId implements Serializable {
      *
      * @param def <code>NodeDef</code> to create identifier for
      */
    NodeDefId(NodeDef def) {
    public NodeDefId(QNodeDefinition def) {
         if (def == null) {
             throw new IllegalArgumentException("NodeDef argument can not be null");
         }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeDefImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeDefImpl.java
deleted file mode 100644
index e52a06520..000000000
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeDefImpl.java
++ /dev/null
@@ -1,273 +0,0 @@
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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.QNodeDefinitionImpl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This class implements the <code>NodeDef</code> interface and additionally
 * provides setter methods for the various node definition attributes.
 */
public class NodeDefImpl extends ItemDefImpl implements NodeDef {

    /**
     * The name of the default primary type.
     */
    private Name defaultPrimaryType;

    /**
     * The names of the required primary types.
     */
    private Set<Name> requiredPrimaryTypes;

    /**
     * The 'allowsSameNameSiblings' flag.
     */
    private boolean allowsSameNameSiblings;

    /**
     * The identifier of this node definition. The identifier is lazily computed
     * based on the characteristics of this node definition and reset on every
     * attribute change.
     */
    private NodeDefId id;

    /**
     * Default constructor.
     */
    public NodeDefImpl() {
        defaultPrimaryType = null;
        requiredPrimaryTypes = new HashSet<Name>();
        requiredPrimaryTypes.add(NameConstants.NT_BASE);
        allowsSameNameSiblings = false;
        id = null;
    }

    public NodeDefImpl(QNodeDefinition nd) {
        super(nd);
        defaultPrimaryType = nd.getDefaultPrimaryType();
        requiredPrimaryTypes = new HashSet<Name>(Arrays.asList(nd.getRequiredPrimaryTypes()));
        allowsSameNameSiblings = nd.allowsSameNameSiblings();
        id = null;
    }

    /**
     * Returns the QNodeDefinition for this NodeDef
     * @return the QNodeDefinition
     */
    public QNodeDefinition getQNodeDefinition() {
        return new QNodeDefinitionImpl(
                getName(),
                getDeclaringNodeType(),
                isAutoCreated(),
                isMandatory(),
                getOnParentVersion(),
                isProtected(),
                getDefaultPrimaryType(),
                getRequiredPrimaryTypes(),
                allowsSameNameSiblings()
        );
    }

    /**
     * Sets the name of default primary type.
     *
     * @param defaultNodeType
     */
    public void setDefaultPrimaryType(Name defaultNodeType) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        this.defaultPrimaryType = defaultNodeType;
    }

    /**
     * Sets the names of the required primary types.
     *
     * @param requiredPrimaryTypes
     */
    public void setRequiredPrimaryTypes(Name[] requiredPrimaryTypes) {
        if (requiredPrimaryTypes == null) {
            throw new IllegalArgumentException("requiredPrimaryTypes can not be null");
        }
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        this.requiredPrimaryTypes.clear();
        this.requiredPrimaryTypes.addAll(Arrays.asList(requiredPrimaryTypes));
    }

    /**
     * Sets the 'allowsSameNameSiblings' flag.
     *
     * @param allowsSameNameSiblings
     */
    public void setAllowsSameNameSiblings(boolean allowsSameNameSiblings) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        this.allowsSameNameSiblings = allowsSameNameSiblings;
    }

    //------------------------------------------------< ItemDefImpl overrides >
    /**
     * {@inheritDoc}
     */
    public void setDeclaringNodeType(Name declaringNodeType) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        super.setDeclaringNodeType(declaringNodeType);
    }

    /**
     * {@inheritDoc}
     */
    public void setName(Name name) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        super.setName(name);
    }

    /**
     * {@inheritDoc}
     */
    public void setAutoCreated(boolean autoCreated) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        super.setAutoCreated(autoCreated);
    }

    /**
     * {@inheritDoc}
     */
    public void setOnParentVersion(int onParentVersion) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        super.setOnParentVersion(onParentVersion);
    }

    /**
     * {@inheritDoc}
     */
    public void setProtected(boolean writeProtected) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        super.setProtected(writeProtected);
    }

    /**
     * {@inheritDoc}
     */
    public void setMandatory(boolean mandatory) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        super.setMandatory(mandatory);
    }

    //--------------------------------------------------------------< NodeDef >
    /**
     * {@inheritDoc}
     * <p/>
     * The identifier is computed based on the characteristics of this node
     * definition, i.e. modifying attributes of this node definition will
     * have impact on the identifier returned by this method.
     */
    public NodeDefId getId() {
        if (id == null) {
            // generate new identifier based on this node definition
            id = new NodeDefId(this);
        }
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public Name getDefaultPrimaryType() {
        return defaultPrimaryType;
    }

    /**
     * {@inheritDoc}
     */
    public Name[] getRequiredPrimaryTypes() {
        if (requiredPrimaryTypes.isEmpty()) {
            return Name.EMPTY_ARRAY;
        }
        return requiredPrimaryTypes.toArray(
                new Name[requiredPrimaryTypes.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public boolean allowsSameNameSiblings() {
        return allowsSameNameSiblings;
    }

    /**
     * {@inheritDoc}
     *
     * @return always <code>true</code>
     */
    public boolean definesNode() {
        return true;
    }

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * Compares two node definitions for equality. Returns <code>true</code>
     * if the given object is a node defintion and has the same attributes
     * as this node definition.
     *
     * @param obj the object to compare this node definition with
     * @return <code>true</code> if the object is equal to this node definition,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NodeDefImpl) {
            NodeDefImpl other = (NodeDefImpl) obj;
            return super.equals(obj)
                    && requiredPrimaryTypes.equals(other.requiredPrimaryTypes)
                    && (defaultPrimaryType == null
                            ? other.defaultPrimaryType == null
                            : defaultPrimaryType.equals(other.defaultPrimaryType))
                    && allowsSameNameSiblings == other.allowsSameNameSiblings;
        }
        return false;
    }

    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }

}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeDefinitionImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeDefinitionImpl.java
deleted file mode 100644
index 19d048f14..000000000
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeDefinitionImpl.java
++ /dev/null
@@ -1,188 +0,0 @@
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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.NamespaceException;

/**
 * This class implements the <code>NodeDefinition</code> interface.
 * All method calls are delegated to the wrapped {@link NodeDef},
 * performing the translation from <code>Name</code>s to JCR names
 * (and vice versa) where necessary.
 */
public class NodeDefinitionImpl extends ItemDefinitionImpl implements NodeDefinition {

    /**
     * Logger instance for this class
     */
    private static Logger log = LoggerFactory.getLogger(NodeDefinitionImpl.class);

    /**
     * Package private constructor.
     *
     * @param nodeDef    child node definition
     * @param ntMgr      node type manager
     * @param resolver   name resolver
     */
    NodeDefinitionImpl(NodeDef nodeDef, NodeTypeManagerImpl ntMgr,
                       NamePathResolver resolver) {
        super(nodeDef, ntMgr, resolver);
    }

    /**
     * Returns the wrapped node definition.
     *
     * @return the wrapped node definition.
     */
    public NodeDef unwrap() {
        return (NodeDef) itemDef;
    }

    //-------------------------------------------------------< NodeDefinition >
    /**
     * {@inheritDoc}
     */
    public NodeType getDefaultPrimaryType() {
        Name ntName = ((NodeDef) itemDef).getDefaultPrimaryType();
        if (ntName == null) {
            return null;
        }
        if (ntMgr == null) {
            return null;
        }
        try {
            return ntMgr.getNodeType(ntName);
        } catch (NoSuchNodeTypeException e) {
            // should never get here
            log.error("invalid default node type " + ntName, e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeType[] getRequiredPrimaryTypes() {
        if (ntMgr == null) {
            return null;
        }
        Name[] ntNames = ((NodeDef) itemDef).getRequiredPrimaryTypes();
        try {
            if (ntNames == null || ntNames.length == 0) {
                // return "nt:base"
                return new NodeType[] {ntMgr.getNodeType(NameConstants.NT_BASE)};
            } else {
                NodeType[] nodeTypes = new NodeType[ntNames.length];
                for (int i = 0; i < ntNames.length; i++) {
                    nodeTypes[i] = ntMgr.getNodeType(ntNames[i]);
                }
                return nodeTypes;
            }
        } catch (NoSuchNodeTypeException e) {
            // should never get here
            log.error("required node type does not exist", e);
            return new NodeType[0];
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean allowsSameNameSiblings() {
        return ((NodeDef) itemDef).allowsSameNameSiblings();
    }

    //--------------------------------------------------< new JSR 283 methods >
    /**
     * Returns the names of the required primary node types.
     * <p/>
     * If this <code>NodeDefinition</code> is acquired from a live
     * <code>NodeType</code> this list will reflect the node types returned by
     * <code>getRequiredPrimaryTypes</code>, above.
     * <p/>
     * If this <code>NodeDefinition</code> is actually a
     * <code>NodeDefinitionTemplate</code> that is not part of a registered node
     * type, then this method will return the required primary types as set in
     * that template. If that template is a newly-created empty one, then this
     * method will return an array containing a single string indicating the
     * node type <code>nt:base</code>.
     *
     * @return a String array
     * @since JCR 2.0
     */
    public String[] getRequiredPrimaryTypeNames() {
        Name[] ntNames = ((NodeDef) itemDef).getRequiredPrimaryTypes();
        try {
            if (ntNames == null || ntNames.length == 0) {
                // return "nt:base"
                return new String[] {resolver.getJCRName(NameConstants.NT_BASE)};
            } else {
                String[] names = new String[ntNames.length];
                for (int i = 0; i < ntNames.length; i++) {
                    names[i] = resolver.getJCRName(ntNames[i]);
                }
                return names;
            }
        } catch (NamespaceException e) {
            // should never get here
            log.error("encountered unregistered namespace in node type name", e);
            return new String[0];
        }
    }

    /**
     * Returns the name of the default primary node type.
     * <p/>
     * If this <code>NodeDefinition</code> is acquired from a live
     * <code>NodeType</code> this list will reflect the NodeType returned by
     * getDefaultPrimaryType, above.
     * <p/>
     * If this <code>NodeDefinition</code> is actually a
     * <code>NodeDefinitionTemplate</code> that is not part of a registered node
     * type, then this method will return the required primary types as set in
     * that template. If that template is a newly-created empty one, then this
     * method will return <code>null</code>.
     *
     * @return a String
     * @since JCR 2.0
     */
    public String getDefaultPrimaryTypeName() {
        Name ntName = ((NodeDef) itemDef).getDefaultPrimaryType();
        if (ntName == null) {
            return null;
        }

        try {
            return resolver.getJCRName(ntName);
        } catch (NamespaceException e) {
            // should never get here
            log.error("encountered unregistered namespace in node type name", e);
            // not correct, but an acceptable fallback
            return ntName.toString();
        }
    }
}

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeDef.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeDef.java
index e61b622c7..32daf9ce0 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeDef.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeDef.java
@@ -54,8 +54,8 @@ public class NodeTypeDef implements Cloneable {
     private boolean abstractStatus;
     private Name primaryItemName;
 
    private Set<PropDef> propDefs;
    private Set<NodeDef> nodeDefs;
    private Set<QPropertyDefinition> propDefs;
    private Set<QNodeDefinition> nodeDefs;
     private Set<Name> dependencies;
 
     /**
@@ -70,8 +70,8 @@ public class NodeTypeDef implements Cloneable {
         orderableChildNodes = false;
         abstractStatus = false;
         queryable = true;
        nodeDefs = new HashSet<NodeDef>();
        propDefs = new HashSet<PropDef>();
        nodeDefs = new HashSet<QNodeDefinition>();
        propDefs = new HashSet<QPropertyDefinition>();
     }
 
     /**
@@ -86,13 +86,13 @@ public class NodeTypeDef implements Cloneable {
         orderableChildNodes = def.hasOrderableChildNodes();
         abstractStatus = def.isAbstract();
         queryable = def.isQueryable();
        nodeDefs = new HashSet<NodeDef>();
        nodeDefs = new HashSet<QNodeDefinition>();
         for (QNodeDefinition nd: def.getChildNodeDefs()) {
            nodeDefs.add(new NodeDefImpl(nd));
            nodeDefs.add(nd);
         }
        propDefs = new HashSet<PropDef>();
        propDefs = new HashSet<QPropertyDefinition>();
         for (QPropertyDefinition pd: def.getPropertyDefs()) {
            propDefs.add(new PropDefImpl(pd));
            propDefs.add(pd);
         }
     }
 
@@ -101,17 +101,6 @@ public class NodeTypeDef implements Cloneable {
      * @return the QNodeTypeDefintion
      */
     public QNodeTypeDefinition getQNodeTypeDefinition() {
        QNodeDefinition[] qNodeDefs = new QNodeDefinition[nodeDefs.size()];
        int i=0;
        for (NodeDef nd: nodeDefs) {
            qNodeDefs[i++] = ((NodeDefImpl) nd).getQNodeDefinition();
        }
        QPropertyDefinition[] qPropDefs = new QPropertyDefinition[propDefs.size()];
        i=0;
        for (PropDef pd: propDefs) {
            qPropDefs[i++] = ((PropDefImpl) pd).getQPropertyDefinition();
        }

         return new QNodeTypeDefinitionImpl(
                 getName(),
                 getSupertypes(),
@@ -121,8 +110,8 @@ public class NodeTypeDef implements Cloneable {
                 isQueryable(),
                 hasOrderableChildNodes(),
                 getPrimaryItemName(),
                qPropDefs,
                qNodeDefs
                propDefs.toArray(new QPropertyDefinition[propDefs.size()]),
                nodeDefs.toArray(new QNodeDefinition[nodeDefs.size()])
         );
     }
 
@@ -144,7 +133,7 @@ public class NodeTypeDef implements Cloneable {
             // supertypes
             dependencies.addAll(Arrays.asList(supertypes));
             // child node definitions
            for (NodeDef nd: nodeDefs) {
            for (QNodeDefinition nd: nodeDefs) {
                 // default primary type
                 Name ntName = nd.getDefaultPrimaryType();
                 if (ntName != null && !name.equals(ntName)) {
@@ -159,7 +148,7 @@ public class NodeTypeDef implements Cloneable {
                 }
             }
             // property definitions
            for (PropDef pd : propDefs) {
            for (QPropertyDefinition pd : propDefs) {
                 // [WEAK]REFERENCE value constraints
                 if (pd.getRequiredType() == PropertyType.REFERENCE
                         || pd.getRequiredType() == PropertyType.WEAKREFERENCE) {
@@ -263,7 +252,7 @@ public class NodeTypeDef implements Cloneable {
      *
      * @param defs An array of <code>PropertyDef</code> objects.
      */
    public void setPropertyDefs(PropDef[] defs) {
    public void setPropertyDefs(QPropertyDefinition[] defs) {
         resetDependencies();
         propDefs.clear();
         propDefs.addAll(Arrays.asList(defs));
@@ -272,9 +261,9 @@ public class NodeTypeDef implements Cloneable {
     /**
      * Sets the child node definitions.
      *
     * @param defs An array of <code>NodeDef</code> objects
     * @param defs An array of <code>QNodeDefinition</code> objects
      */
    public void setChildNodeDefs(NodeDef[] defs) {
    public void setChildNodeDefs(QNodeDefinition[] defs) {
         resetDependencies();
         nodeDefs.clear();
         nodeDefs.addAll(Arrays.asList(defs));
@@ -362,11 +351,11 @@ public class NodeTypeDef implements Cloneable {
      * @return an array containing the property definitions or
      *         <code>null</code> if not set.
      */
    public PropDef[] getPropertyDefs() {
    public QPropertyDefinition[] getPropertyDefs() {
         if (propDefs.isEmpty()) {
            return PropDef.EMPTY_ARRAY;
            return QPropertyDefinition.EMPTY_ARRAY;
         }
        return propDefs.toArray(new PropDef[propDefs.size()]);
        return propDefs.toArray(new QPropertyDefinition[propDefs.size()]);
     }
 
     /**
@@ -376,11 +365,11 @@ public class NodeTypeDef implements Cloneable {
      * @return an array containing the child node definitions or
      *         <code>null</code> if not set.
      */
    public NodeDef[] getChildNodeDefs() {
    public QNodeDefinition[] getChildNodeDefs() {
         if (nodeDefs.isEmpty()) {
            return NodeDef.EMPTY_ARRAY;
            return QNodeDefinition.EMPTY_ARRAY;
         }
        return nodeDefs.toArray(new NodeDef[nodeDefs.size()]);
        return nodeDefs.toArray(new QNodeDefinition[nodeDefs.size()]);
     }
 
     //-------------------------------------------< java.lang.Object overrides >
@@ -393,10 +382,10 @@ public class NodeTypeDef implements Cloneable {
         clone.orderableChildNodes = orderableChildNodes;
         clone.abstractStatus = abstractStatus;
         clone.queryable = queryable;
        clone.nodeDefs = new HashSet<NodeDef>();
        clone.nodeDefs = new HashSet<QNodeDefinition>();
         // todo: itemdefs should be cloned as well, since mutable
        clone.nodeDefs = new HashSet<NodeDef>(nodeDefs);
        clone.propDefs = new HashSet<PropDef>(propDefs);
        clone.nodeDefs = new HashSet<QNodeDefinition>(nodeDefs);
        clone.propDefs = new HashSet<QPropertyDefinition>(propDefs);
         return clone;
     }
 
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeDefDiff.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeDefDiff.java
index 53487cd33..8324c91f3 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeDefDiff.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeDefDiff.java
@@ -27,6 +27,9 @@ import java.util.Map;
 import javax.jcr.PropertyType;
 
 import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
 
 /**
  * A <code>NodeTypeDefDiff</code> represents the result of the comparison of
@@ -97,7 +100,7 @@ public class NodeTypeDefDiff {
     private final NodeTypeDef newDef;
     private int type;
 
    private List propDefDiffs = new ArrayList();
    private List<PropDefDiff> propDefDiffs = new ArrayList<PropDefDiff>();
     private List childNodeDefDiffs = new ArrayList();
 
     /**
@@ -249,45 +252,38 @@ public class NodeTypeDefDiff {
          */
 
         int maxType = NONE;
        PropDef[] pda1 = oldDef.getPropertyDefs();
        HashMap defs1 = new HashMap();
        for (int i = 0; i < pda1.length; i++) {
            defs1.put(pda1[i].getId(), pda1[i]);
        Map<PropDefId, QPropertyDefinition> oldDefs = new HashMap<PropDefId, QPropertyDefinition>();
        for (QPropertyDefinition def : oldDef.getPropertyDefs()) {
            oldDefs.put(new PropDefId(def), def);
         }
 
        PropDef[] pda2 = newDef.getPropertyDefs();
        HashMap defs2 = new HashMap();
        for (int i = 0; i < pda2.length; i++) {
            defs2.put(pda2[i].getId(), pda2[i]);
        Map<PropDefId, QPropertyDefinition> newDefs = new HashMap<PropDefId, QPropertyDefinition>();
        for (QPropertyDefinition def : newDef.getPropertyDefs()) {
            newDefs.put(new PropDefId(def), def);
         }
 
         /**
          * walk through defs1 and process all entries found in
          * both defs1 & defs2 and those found only in defs1
          */
        Iterator iter = defs1.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            PropDefId id = (PropDefId) entry.getKey();
            PropDef def1 = (PropDef) entry.getValue();
            PropDef def2 = (PropDef) defs2.get(id);
        for (Map.Entry<PropDefId, QPropertyDefinition> entry : oldDefs.entrySet()) {
            PropDefId id = entry.getKey();
            QPropertyDefinition def1 = entry.getValue();
            QPropertyDefinition def2 = newDefs.get(id);
             PropDefDiff diff = new PropDefDiff(def1, def2);
             if (diff.getType() > maxType) {
                 maxType = diff.getType();
             }
             propDefDiffs.add(diff);
            defs2.remove(id);
            newDefs.remove(id);
         }
 
         /**
          * defs2 by now only contains entries found in defs2 only;
          * walk through defs2 and process all remaining entries
          */
        iter = defs2.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            PropDefId id = (PropDefId) entry.getKey();
            PropDef def = (PropDef) entry.getValue();
        for (Map.Entry<PropDefId, QPropertyDefinition> entry : newDefs.entrySet()) {
            QPropertyDefinition def = entry.getValue();
             PropDefDiff diff = new PropDefDiff(null, def);
             if (diff.getType() > maxType) {
                 maxType = diff.getType();
@@ -308,16 +304,16 @@ public class NodeTypeDefDiff {
          */
 
         int maxType = NONE;
        NodeDef[] cnda1 = oldDef.getChildNodeDefs();
        QNodeDefinition[] cnda1 = oldDef.getChildNodeDefs();
         HashMap defs1 = new HashMap();
         for (int i = 0; i < cnda1.length; i++) {
            defs1.put(cnda1[i].getId(), cnda1[i]);
            defs1.put(new NodeDefId(cnda1[i]), cnda1[i]);
         }
 
        NodeDef[] cnda2 = newDef.getChildNodeDefs();
        QNodeDefinition[] cnda2 = newDef.getChildNodeDefs();
         HashMap defs2 = new HashMap();
         for (int i = 0; i < cnda2.length; i++) {
            defs2.put(cnda2[i].getId(), cnda2[i]);
            defs2.put(new NodeDefId(cnda2[i]), cnda2[i]);
         }
 
         /**
@@ -328,8 +324,8 @@ public class NodeTypeDefDiff {
         while (iter.hasNext()) {
             Map.Entry entry = (Map.Entry) iter.next();
             NodeDefId id = (NodeDefId) entry.getKey();
            NodeDef def1 = (NodeDef) entry.getValue();
            NodeDef def2 = (NodeDef) defs2.get(id);
            QItemDefinition def1 = (QItemDefinition) entry.getValue();
            QItemDefinition def2 = (QItemDefinition) defs2.get(id);
             ChildNodeDefDiff diff = new ChildNodeDefDiff(def1, def2);
             if (diff.getType() > maxType) {
                 maxType = diff.getType();
@@ -346,7 +342,7 @@ public class NodeTypeDefDiff {
         while (iter.hasNext()) {
             Map.Entry entry = (Map.Entry) iter.next();
             NodeDefId id = (NodeDefId) entry.getKey();
            NodeDef def = (NodeDef) entry.getValue();
            QItemDefinition def = (QItemDefinition) entry.getValue();
             ChildNodeDefDiff diff = new ChildNodeDefDiff(null, def);
             if (diff.getType() > maxType) {
                 maxType = diff.getType();
@@ -408,11 +404,11 @@ public class NodeTypeDefDiff {
     //--------------------------------------------------------< inner classes >
 
     abstract class ChildItemDefDiff {
        protected final ItemDef oldDef;
        protected final ItemDef newDef;
        protected final QItemDefinition oldDef;
        protected final QItemDefinition newDef;
         protected int type;
 
        ChildItemDefDiff(ItemDef oldDef, ItemDef newDef) {
        ChildItemDefDiff(QItemDefinition oldDef, QItemDefinition newDef) {
             this.oldDef = oldDef;
             this.newDef = newDef;
             init();
@@ -495,7 +491,7 @@ public class NodeTypeDefDiff {
                 operationString = "NONE";
             }
 
            ItemDef itemDefinition = (oldDef != null) ? oldDef : newDef;
            QItemDefinition itemDefinition = (oldDef != null) ? oldDef : newDef;
 
             return getClass().getName() + "[itemName="
                     + itemDefinition.getName() + ", type=" + typeString
@@ -506,16 +502,16 @@ public class NodeTypeDefDiff {
 
     public class PropDefDiff extends ChildItemDefDiff {
 
        PropDefDiff(PropDef oldDef, PropDef newDef) {
        PropDefDiff(QPropertyDefinition oldDef, QPropertyDefinition newDef) {
             super(oldDef, newDef);
         }
 
        public PropDef getOldDef() {
            return (PropDef) oldDef;
        public QPropertyDefinition getOldDef() {
            return (QPropertyDefinition) oldDef;
         }
 
        public PropDef getNewDef() {
            return (PropDef) newDef;
        public QPropertyDefinition getNewDef() {
            return (QPropertyDefinition) newDef;
         }
 
         protected void init() {
@@ -584,16 +580,16 @@ public class NodeTypeDefDiff {
 
     public class ChildNodeDefDiff extends ChildItemDefDiff {
 
        ChildNodeDefDiff(NodeDef oldDef, NodeDef newDef) {
        ChildNodeDefDiff(QItemDefinition oldDef, QItemDefinition newDef) {
             super(oldDef, newDef);
         }
 
        public NodeDef getOldDef() {
            return (NodeDef) oldDef;
        public QNodeDefinition getOldDef() {
            return (QNodeDefinition) oldDef;
         }
 
        public NodeDef getNewDef() {
            return (NodeDef) newDef;
        public QNodeDefinition getNewDef() {
            return (QNodeDefinition) newDef;
         }
 
         protected void init() {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeDefinitionImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeDefinitionImpl.java
index 5c92552b0..1013ade75 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeDefinitionImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeDefinitionImpl.java
@@ -18,7 +18,11 @@ package org.apache.jackrabbit.core.nodetype;
 
 import javax.jcr.nodetype.NodeTypeDefinition;
 import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QItemDefinition;
 import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -163,7 +167,7 @@ public class NodeTypeDefinitionImpl implements NodeTypeDefinition {
      * {@inheritDoc}
      */
     public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        NodeDef[] cnda = ntd.getChildNodeDefs();
        QItemDefinition[] cnda = ntd.getChildNodeDefs();
         NodeDefinition[] nodeDefs = new NodeDefinition[cnda.length];
         for (int i = 0; i < cnda.length; i++) {
             nodeDefs[i] = new NodeDefinitionImpl(cnda[i], null, resolver);
@@ -175,7 +179,7 @@ public class NodeTypeDefinitionImpl implements NodeTypeDefinition {
      * {@inheritDoc}
      */
     public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        PropDef[] pda = ntd.getPropertyDefs();
        QPropertyDefinition[] pda = ntd.getPropertyDefs();
         PropertyDefinition[] propDefs = new PropertyDefinition[pda.length];
         for (int i = 0; i < pda.length; i++) {
             propDefs[i] = new PropertyDefinitionImpl(pda[i], null, resolver, valueFactory);
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeImpl.java
index 8a0041514..b704d6517 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeImpl.java
@@ -34,6 +34,8 @@ import javax.jcr.nodetype.PropertyDefinition;
 import org.apache.jackrabbit.core.data.DataStore;
 import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
 import org.apache.jackrabbit.spi.commons.conversion.NameException;
 import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
 import org.apache.jackrabbit.spi.commons.nodetype.AbstractNodeType;
@@ -123,10 +125,10 @@ public class NodeTypeImpl extends AbstractNodeType implements NodeType, NodeType
      * @see NodeDefinition#isAutoCreated
      */
     public NodeDefinition[] getAutoCreatedNodeDefinitions() {
        NodeDef[] cnda = ent.getAutoCreateNodeDefs();
        QNodeDefinition[] cnda = ent.getAutoCreateNodeDefs();
         NodeDefinition[] nodeDefs = new NodeDefinition[cnda.length];
         for (int i = 0; i < cnda.length; i++) {
            nodeDefs[i] = ntMgr.getNodeDefinition(cnda[i].getId());
            nodeDefs[i] = ntMgr.getNodeDefinition(cnda[i]);
         }
         return nodeDefs;
     }
@@ -141,10 +143,10 @@ public class NodeTypeImpl extends AbstractNodeType implements NodeType, NodeType
      * @see PropertyDefinition#isAutoCreated
      */
     public PropertyDefinition[] getAutoCreatedPropertyDefinitions() {
        PropDef[] pda = ent.getAutoCreatePropDefs();
        QPropertyDefinition[] pda = ent.getAutoCreatePropDefs();
         PropertyDefinition[] propDefs = new PropertyDefinition[pda.length];
         for (int i = 0; i < pda.length; i++) {
            propDefs[i] = ntMgr.getPropertyDefinition(pda[i].getId());
            propDefs[i] = ntMgr.getPropertyDefinition(pda[i]);
         }
         return propDefs;
     }
@@ -159,10 +161,10 @@ public class NodeTypeImpl extends AbstractNodeType implements NodeType, NodeType
      * @see PropertyDefinition#isMandatory
      */
     public PropertyDefinition[] getMandatoryPropertyDefinitions() {
        PropDef[] pda = ent.getMandatoryPropDefs();
        QPropertyDefinition[] pda = ent.getMandatoryPropDefs();
         PropertyDefinition[] propDefs = new PropertyDefinition[pda.length];
         for (int i = 0; i < pda.length; i++) {
            propDefs[i] = ntMgr.getPropertyDefinition(pda[i].getId());
            propDefs[i] = ntMgr.getPropertyDefinition(pda[i]);
         }
         return propDefs;
     }
@@ -177,10 +179,10 @@ public class NodeTypeImpl extends AbstractNodeType implements NodeType, NodeType
      * @see NodeDefinition#isMandatory
      */
     public NodeDefinition[] getMandatoryNodeDefinitions() {
        NodeDef[] cnda = ent.getMandatoryNodeDefs();
        QNodeDefinition[] cnda = ent.getMandatoryNodeDefs();
         NodeDefinition[] nodeDefs = new NodeDefinition[cnda.length];
         for (int i = 0; i < cnda.length; i++) {
            nodeDefs[i] = ntMgr.getNodeDefinition(cnda[i].getId());
            nodeDefs[i] = ntMgr.getNodeDefinition(cnda[i]);
         }
         return nodeDefs;
     }
@@ -349,10 +351,10 @@ public class NodeTypeImpl extends AbstractNodeType implements NodeType, NodeType
      * {@inheritDoc}
      */
     public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        NodeDef[] cnda = ntd.getChildNodeDefs();
        QNodeDefinition[] cnda = ntd.getChildNodeDefs();
         NodeDefinition[] nodeDefs = new NodeDefinition[cnda.length];
         for (int i = 0; i < cnda.length; i++) {
            nodeDefs[i] = ntMgr.getNodeDefinition(cnda[i].getId());
            nodeDefs[i] = ntMgr.getNodeDefinition(cnda[i]);
         }
         return nodeDefs;
     }
@@ -397,10 +399,10 @@ public class NodeTypeImpl extends AbstractNodeType implements NodeType, NodeType
      * {@inheritDoc}
      */
     public NodeDefinition[] getChildNodeDefinitions() {
        NodeDef[] cnda = ent.getAllNodeDefs();
        QNodeDefinition[] cnda = ent.getAllNodeDefs();
         NodeDefinition[] nodeDefs = new NodeDefinition[cnda.length];
         for (int i = 0; i < cnda.length; i++) {
            nodeDefs[i] = ntMgr.getNodeDefinition(cnda[i].getId());
            nodeDefs[i] = ntMgr.getNodeDefinition(cnda[i]);
         }
         return nodeDefs;
     }
@@ -409,10 +411,10 @@ public class NodeTypeImpl extends AbstractNodeType implements NodeType, NodeType
      * {@inheritDoc}
      */
     public PropertyDefinition[] getPropertyDefinitions() {
        PropDef[] pda = ent.getAllPropDefs();
        QPropertyDefinition[] pda = ent.getAllPropDefs();
         PropertyDefinition[] propDefs = new PropertyDefinition[pda.length];
         for (int i = 0; i < pda.length; i++) {
            propDefs[i] = ntMgr.getPropertyDefinition(pda[i].getId());
            propDefs[i] = ntMgr.getPropertyDefinition(pda[i]);
         }
         return propDefs;
     }
@@ -427,7 +429,7 @@ public class NodeTypeImpl extends AbstractNodeType implements NodeType, NodeType
         }
         try {
             Name name = resolver.getQName(propertyName);
            PropDef def;
            QPropertyDefinition def;
             try {
                 // try to get definition that matches the given value type
                 def = ent.getApplicablePropertyDef(name, value.getType(), false);
@@ -498,7 +500,7 @@ public class NodeTypeImpl extends AbstractNodeType implements NodeType, NodeType
                     return false;
                 }
             }
            PropDef def;
            QPropertyDefinition def;
             try {
                 // try to get definition that matches the given value type
                 def = ent.getApplicablePropertyDef(name, type, true);
@@ -607,10 +609,10 @@ public class NodeTypeImpl extends AbstractNodeType implements NodeType, NodeType
      * {@inheritDoc}
      */
     public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        PropDef[] pda = ntd.getPropertyDefs();
        QPropertyDefinition[] pda = ntd.getPropertyDefs();
         PropertyDefinition[] propDefs = new PropertyDefinition[pda.length];
         for (int i = 0; i < pda.length; i++) {
            propDefs[i] = ntMgr.getPropertyDefinition(pda[i].getId());
            propDefs[i] = ntMgr.getPropertyDefinition(pda[i]);
         }
         return propDefs;
     }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeManagerImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeManagerImpl.java
index 625f401e7..08c35a874 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeManagerImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeManagerImpl.java
@@ -61,13 +61,17 @@ import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.QNodeTypeDefinition;
 import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
 import org.apache.jackrabbit.spi.commons.conversion.NameException;
 import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
 import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.nodetype.AbstractNodeTypeManager;
import org.apache.jackrabbit.spi.commons.nodetype.InvalidConstraintException;
import org.apache.jackrabbit.spi.commons.nodetype.*;
import org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl;
 import org.apache.jackrabbit.spi.commons.nodetype.QDefinitionBuilderFactory;
 import org.apache.jackrabbit.spi.commons.nodetype.constraint.ValueConstraint;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
 import org.xml.sax.InputSource;
 import org.xml.sax.SAXException;
 
@@ -108,13 +112,13 @@ public class NodeTypeManagerImpl extends AbstractNodeTypeManager implements Jack
      * A cache for <code>PropertyDefinition</code> instances created by this
      * <code>NodeTypeManager</code>
      */
    private final Map<PropDefId, PropertyDefinitionImpl> pdCache;
    private final Map<QPropertyDefinition, PropertyDefinitionImpl> pdCache;
 
     /**
      * A cache for <code>NodeDefinition</code> instances created by this
      * <code>NodeTypeManager</code>
      */
    private final Map<NodeDefId, NodeDefinitionImpl> ndCache;
    private final Map<QNodeDefinition, NodeDefinitionImpl> ndCache;
 
     private final DataStore store;
 
@@ -144,7 +148,7 @@ public class NodeTypeManagerImpl extends AbstractNodeTypeManager implements Jack
 
         rootNodeDef =
             new NodeDefinitionImpl(ntReg.getRootNodeDef(), this, session);
        ndCache.put(rootNodeDef.unwrap().getId(), rootNodeDef);
        ndCache.put(rootNodeDef.unwrap(), rootNodeDef);
     }
 
     /**
@@ -155,36 +159,30 @@ public class NodeTypeManagerImpl extends AbstractNodeTypeManager implements Jack
     }
 
     /**
     * @param id node def id
     * @param def the QNodeDefinition
      * @return the node definition
      */
    public NodeDefinitionImpl getNodeDefinition(NodeDefId id) {
    public NodeDefinitionImpl getNodeDefinition(QNodeDefinition def) {
         synchronized (ndCache) {
            NodeDefinitionImpl ndi = ndCache.get(id);
            NodeDefinitionImpl ndi = ndCache.get(def);
             if (ndi == null) {
                NodeDef nd = ntReg.getNodeDef(id);
                if (nd != null) {
                    ndi = new NodeDefinitionImpl(nd, this, session);
                    ndCache.put(id, ndi);
                }
                ndi = new NodeDefinitionImpl(def, this, session);
                ndCache.put(def, ndi);
             }
             return ndi;
         }
     }
 
     /**
     * @param id prop def id
     * @param def prop def
      * @return the property definition
      */
    public PropertyDefinitionImpl getPropertyDefinition(PropDefId id) {
    public PropertyDefinitionImpl getPropertyDefinition(QPropertyDefinition def) {
         synchronized (pdCache) {
            PropertyDefinitionImpl pdi = pdCache.get(id);
            PropertyDefinitionImpl pdi = pdCache.get(def);
             if (pdi == null) {
                PropDef pd = ntReg.getPropDef(id);
                if (pd != null) {
                    pdi = new PropertyDefinitionImpl(pd, this, session, valueFactory);
                    pdCache.put(id, pdi);
                }
                pdi = new PropertyDefinitionImpl(def, this, session, valueFactory);
                pdCache.put(def, pdi);
             }
             return pdi;
         }
@@ -351,7 +349,7 @@ public class NodeTypeManagerImpl extends AbstractNodeTypeManager implements Jack
         synchronized (ndCache) {
             Iterator iter = ndCache.values().iterator();
             while (iter.hasNext()) {
                NodeDefinitionImpl nd = (NodeDefinitionImpl) iter.next();
                NodeDefinitionImpl nd = (org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl) iter.next();
                 if (ntName.equals(nd.unwrap().getDeclaringNodeType())) {
                     iter.remove();
                 }
@@ -695,16 +693,16 @@ public class NodeTypeManagerImpl extends AbstractNodeTypeManager implements Jack
         // child nodes
         NodeDefinition[] ndefs = definition.getDeclaredChildNodeDefinitions();
         if (ndefs != null) {
            NodeDef[] qndefs = new NodeDef[ndefs.length];
            QNodeDefinition[] qndefs = new QNodeDefinition[ndefs.length];
             for (int i = 0; i < ndefs.length; i++) {
                NodeDefImpl qndef = new NodeDefImpl();
                QNodeDefinitionBuilder qndef = new QNodeDefinitionBuilder();
                 // declaring node type
                 qndef.setDeclaringNodeType(def.getName());
                 // name
                 name = ndefs[i].getName();
                 if (name != null) {
                     if (name.equals("*")) {
                        qndef.setName(ItemDef.ANY_NAME);
                        qndef.setName(NameConstants.ANY_NAME);
                     } else {
                         try {
                             qndef.setName(session.getQName(name));
@@ -747,7 +745,7 @@ public class NodeTypeManagerImpl extends AbstractNodeTypeManager implements Jack
                 qndef.setOnParentVersion(ndefs[i].getOnParentVersion());
                 qndef.setAllowsSameNameSiblings(ndefs[i].allowsSameNameSiblings());
 
                qndefs[i] = qndef;
                qndefs[i] = qndef.build();
             }
             def.setChildNodeDefs(qndefs);
         }
@@ -755,16 +753,16 @@ public class NodeTypeManagerImpl extends AbstractNodeTypeManager implements Jack
         // properties
         PropertyDefinition[] pdefs = definition.getDeclaredPropertyDefinitions();
         if (pdefs != null) {
            PropDef[] qpdefs = new PropDef[pdefs.length];
            QPropertyDefinition[] qpdefs = new QPropertyDefinition[pdefs.length];
             for (int i = 0; i < pdefs.length; i++) {
                PropDefImpl qpdef = new PropDefImpl();
                QPropertyDefinitionBuilder qpdef = new QPropertyDefinitionBuilder();
                 // declaring node type
                 qpdef.setDeclaringNodeType(def.getName());
                 // name
                 name = pdefs[i].getName();
                 if (name != null) {
                     if (name.equals("*")) {
                        qpdef.setName(ItemDef.ANY_NAME);
                        qpdef.setName(NameConstants.ANY_NAME);
                     } else {
                         try {
                             qpdef.setName(session.getQName(name));
@@ -812,7 +810,7 @@ public class NodeTypeManagerImpl extends AbstractNodeTypeManager implements Jack
                     qpdef.setDefaultValues(qvalues);
                 }
 
                qpdefs[i] = qpdef;
                qpdefs[i] = qpdef.build();
             }
             def.setPropertyDefs(qpdefs);
         }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeRegistry.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeRegistry.java
index d5a969085..4b4adf6bc 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeRegistry.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/NodeTypeRegistry.java
@@ -47,11 +47,14 @@ import org.apache.jackrabbit.core.fs.FileSystem;
 import org.apache.jackrabbit.core.fs.FileSystemException;
 import org.apache.jackrabbit.core.fs.FileSystemResource;
 import org.apache.jackrabbit.core.util.Dumpable;
import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
 import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.nodetype.QNodeDefinitionBuilder;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -69,8 +72,6 @@ public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {
     private static final String CUSTOM_NODETYPES_RESOURCE_NAME =
             "custom_nodetypes.xml";
 
    // file system where node type registrations are persisted
    private final FileSystem ntStore;
     /**
      * resource holding custom node type definitions which are represented as
      * nodes in the repository; it is needed in order to make the registrations
@@ -89,12 +90,7 @@ public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {
     private final Map<Name, NodeTypeDef> registeredNTDefs;
 
     // definition of the root node
    private final NodeDef rootNodeDef;

    // map of id's and property definitions
    private final Map<PropDefId, PropDef> propDefs;
    // map of id's and node definitions
    private final Map<NodeDefId, NodeDef> nodeDefs;
    private final QNodeDefinition rootNodeDef;
 
     /**
      * namespace registry for resolving prefixes and namespace URI's;
@@ -557,22 +553,6 @@ public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {
         return builtInNTDefs.contains(nodeTypeName);
     }
 
    /**
     * @param id node def id
     * @return the node definition for the given id.
     */
    public NodeDef getNodeDef(NodeDefId id) {
        return nodeDefs.get(id);
    }

    /**
     * @param id property def id
     * @return the property definition for the given id.
     */
    public PropDef getPropDef(PropDefId id) {
        return propDefs.get(id);
    }

     /**
      * Add a <code>NodeTypeRegistryListener</code>
      *
@@ -613,10 +593,10 @@ public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {
             ps.println("\tMixin\t" + ntd.isMixin());
             ps.println("\tOrderableChildNodes\t" + ntd.hasOrderableChildNodes());
             ps.println("\tPrimaryItemName\t" + (ntd.getPrimaryItemName() == null ? "<null>" : ntd.getPrimaryItemName().toString()));
            PropDef[] pd = ntd.getPropertyDefs();
            for (PropDef aPd : pd) {
            QPropertyDefinition[] pd = ntd.getPropertyDefs();
            for (QPropertyDefinition aPd : pd) {
                 ps.print("\tPropertyDefinition");
                ps.println(" (declared in " + aPd.getDeclaringNodeType() + ") id=" + aPd.getId());
                ps.println(" (declared in " + aPd.getDeclaringNodeType() + ")");
                 ps.println("\t\tName\t\t" + (aPd.definesResidual() ? "*" : aPd.getName().toString()));
                 String type = aPd.getRequiredType() == 0 ? "null" : PropertyType.nameFromValue(aPd.getRequiredType());
                 ps.println("\t\tRequiredType\t" + type);
@@ -633,12 +613,12 @@ public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {
                     }
                 }
                 ps.println("\t\tValueConstraints\t" + constraints.toString());
                InternalValue[] defVals = aPd.getDefaultValues();
                QValue[] defVals = aPd.getDefaultValues();
                 StringBuffer defaultValues = new StringBuffer();
                 if (defVals == null) {
                     defaultValues.append("<null>");
                 } else {
                    for (InternalValue defVal : defVals) {
                    for (QValue defVal : defVals) {
                         if (defaultValues.length() > 0) {
                             defaultValues.append(", ");
                         }
@@ -652,10 +632,10 @@ public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {
                 ps.println("\t\tProtected\t" + aPd.isProtected());
                 ps.println("\t\tMultiple\t" + aPd.isMultiple());
             }
            NodeDef[] nd = ntd.getChildNodeDefs();
            for (NodeDef aNd : nd) {
            QNodeDefinition[] nd = ntd.getChildNodeDefs();
            for (QNodeDefinition aNd : nd) {
                 ps.print("\tNodeDefinition");
                ps.println(" (declared in " + aNd.getDeclaringNodeType() + ") id=" + aNd.getId());
                ps.println(" (declared in " + aNd.getDeclaringNodeType() + ")");
                 ps.println("\t\tName\t\t" + (aNd.definesResidual() ? "*" : aNd.getName().toString()));
                 Name[] reqPrimaryTypes = aNd.getRequiredPrimaryTypes();
                 if (reqPrimaryTypes != null && reqPrimaryTypes.length > 0) {
@@ -721,9 +701,8 @@ public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {
     protected NodeTypeRegistry(NamespaceRegistry nsReg, FileSystem ntStore)
             throws RepositoryException {
         this.nsReg = nsReg;
        this.ntStore = ntStore;
         customNodeTypesResource =
                new FileSystemResource(this.ntStore, CUSTOM_NODETYPES_RESOURCE_NAME);
                new FileSystemResource(ntStore, CUSTOM_NODETYPES_RESOURCE_NAME);
         try {
             // make sure path to resource exists
             if (!customNodeTypesResource.exists()) {
@@ -741,12 +720,9 @@ public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {
         // for the old one)
         entCache = new BitsetENTCacheImpl();
         registeredNTDefs = new ConcurrentReaderHashMap();
        propDefs = new ConcurrentReaderHashMap();
        nodeDefs = new ConcurrentReaderHashMap();
 
         // setup definition of root node
         rootNodeDef = createRootNodeDef();
        nodeDefs.put(rootNodeDef.getId(), rootNodeDef);
 
         // load and register pre-defined (i.e. built-in) node types
         builtInNTDefs = new NodeTypeDefStore();
@@ -979,7 +955,7 @@ public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {
     /**
      * @return the definition of the root node
      */
    public NodeDef getRootNodeDef() {
    public QNodeDefinition getRootNodeDef() {
         return rootNodeDef;
     }
 
@@ -1183,8 +1159,8 @@ public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {
             }
         }
 
        NodeDef[] nodeDefs = childNodeENT.getAutoCreateNodeDefs();
        for (NodeDef nodeDef : nodeDefs) {
        QNodeDefinition[] nodeDefs = childNodeENT.getAutoCreateNodeDefs();
        for (QNodeDefinition nodeDef : nodeDefs) {
             Name dnt = nodeDef.getDefaultPrimaryType();
             Name definingNT = nodeDef.getDeclaringNodeType();
             try {
@@ -1224,16 +1200,6 @@ public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {
         ntd = (NodeTypeDef) ntd.clone();
         registeredNTDefs.put(name, ntd);
 
        // store property & child node definitions of new node type by id
        PropDef[] pda = ntd.getPropertyDefs();
        for (PropDef aPda : pda) {
            propDefs.put(aPda.getId(), aPda);
        }
        NodeDef[] nda = ntd.getChildNodeDefs();
        for (NodeDef aNda : nda) {
            nodeDefs.put(aNda.getId(), aNda);
        }

         return ent;
     }
 
@@ -1309,15 +1275,6 @@ public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {
             // register clone of node type definition
             ntd = (NodeTypeDef) ntd.clone();
             registeredNTDefs.put(ntd.getName(), ntd);
            // store property & child node definitions of new node type by id
            PropDef[] pda = ntd.getPropertyDefs();
            for (PropDef aPda : pda) {
                propDefs.put(aPda.getId(), aPda);
            }
            NodeDef[] nda = ntd.getChildNodeDefs();
            for (NodeDef aNda : nda) {
                nodeDefs.put(aNda.getId(), aNda);
            }
         }
 
         // finally add newly created effective node types to entCache
@@ -1331,16 +1288,6 @@ public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {
         }
         registeredNTDefs.remove(name);
         entCache.invalidate(name);

        // remove property & child node definitions
        PropDef[] pda = ntd.getPropertyDefs();
        for (PropDef aPda : pda) {
            propDefs.remove(aPda.getId());
        }
        NodeDef[] nda = ntd.getChildNodeDefs();
        for (NodeDef aNda : nda) {
            nodeDefs.remove(aNda.getId());
        }
     }
 
     private void internalUnregister(Collection<Name> ntNames)
@@ -1525,8 +1472,8 @@ public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {
         checkNamespace(ntd.getPrimaryItemName(), nsReg);
 
         // validate property definitions
        PropDef[] pda = ntd.getPropertyDefs();
        for (PropDef pd : pda) {
        QPropertyDefinition[] pda = ntd.getPropertyDefs();
        for (QPropertyDefinition pd : pda) {
             /**
              * sanity check:
              * make sure declaring node type matches name of node type definition
@@ -1557,10 +1504,10 @@ public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {
              * check default values:
              * make sure type of value is consistent with required property type
              */
            InternalValue[] defVals = pd.getDefaultValues();
            QValue[] defVals = pd.getDefaultValues();
             if (defVals != null && defVals.length != 0) {
                 int reqType = pd.getRequiredType();
                for (InternalValue defVal : defVals) {
                for (QValue defVal : defVals) {
                     if (reqType == PropertyType.UNDEFINED) {
                         reqType = defVal.getType();
                     } else {
@@ -1590,7 +1537,7 @@ public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {
             if (constraints != null && constraints.length > 0) {
                 if (defVals != null && defVals.length > 0) {
                     // check value constraints on every value
                    for (InternalValue defVal : defVals) {
                    for (QValue defVal : defVals) {
                         // constraints are OR-ed together
                         boolean satisfied = false;
                         ConstraintViolationException cve = null;
@@ -1638,8 +1585,8 @@ public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {
         }
 
         // validate child-node definitions
        NodeDef[] cnda = ntd.getChildNodeDefs();
        for (NodeDef cnd : cnda) {
        QNodeDefinition[] cnda = ntd.getChildNodeDefs();
        for (QNodeDefinition cnd : cnda) {
             /**
              * sanity check:
              * make sure declaring node type matches name of node type definition
@@ -1816,8 +1763,8 @@ public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {
         return ent;
     }
 
    private static NodeDef createRootNodeDef() {
        NodeDefImpl def = new NodeDefImpl();
    private static QNodeDefinition createRootNodeDef() {
        QNodeDefinitionBuilder def = new QNodeDefinitionBuilder();
 
         // FIXME need a fake declaring node type:
         // rep:root is not quite correct but better than a non-existing node type
@@ -1829,7 +1776,7 @@ public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {
         def.setOnParentVersion(OnParentVersionAction.VERSION);
         def.setAllowsSameNameSiblings(false);
         def.setAutoCreated(true);
        return def;
        return def.build();
     }
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/PropDef.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/PropDef.java
deleted file mode 100644
index 07be11339..000000000
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/PropDef.java
++ /dev/null
@@ -1,89 +0,0 @@
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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.QValueConstraint;

/**
 * <code>PropDef</code> is the internal representation of
 * a property definition. It refers to <code>Name</code>s only
 * and is thus isolated from session-specific namespace mappings.
 *
 * @see javax.jcr.nodetype.PropertyDefinition
 */
public interface PropDef extends ItemDef {

    PropDef[] EMPTY_ARRAY = new PropDef[0];

    /**
     * Returns an identifier for this property definition.
     *
     * @return an identifier for this property definition.
     */
    PropDefId getId();

    /**
     * Returns the required type.
     *
     * @return the required type.
     */
    int getRequiredType();

    /**
     * Returns the array of value constraints.
     *
     * @return the array of value constraints.
     */
    QValueConstraint[] getValueConstraints();

    /**
     * Returns the array of default values.
     *
     * @return the array of default values.
     */
    InternalValue[] getDefaultValues();

    /**
     * Reports whether this property can have multiple values.
     *
     * @return the 'multiple' flag.
     */
    boolean isMultiple();

    /**
     * Returns the array of available query operators.
     *
     * @return the array of query operators.
     */
    String[] getAvailableQueryOperators();

    /**
     * Reports whether this property is full-text searchable.
     *
     * @return the 'fullTextSearchable' flag.
     */
    boolean isFullTextSearchable();

    /**
     * Reports whether this property is query-orderable.
     *
     * @return the 'queryOrderable' flag.
     */
    boolean isQueryOrderable();

}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/PropDefId.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/PropDefId.java
index 667a18f9a..af9ea2784 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/PropDefId.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/PropDefId.java
@@ -18,14 +18,16 @@ package org.apache.jackrabbit.core.nodetype;
 
 import java.io.Serializable;
 
import org.apache.jackrabbit.spi.QPropertyDefinition;

 /**
 * <code>PropDefId</code> serves as identifier for a given <code>PropDef</code>.
 * <code>PropDefId</code> serves as identifier for a given <code>QPropertyDefinition</code>.
  *
  *
 * uniquely identifies a <code>PropDef</code> in the
 * uniquely identifies a <code>QPropertyDefinition</code> in the
  * node type registry.
  */
public class PropDefId implements Serializable {
class PropDefId implements Serializable {
 
     /**
      * Serialization UID of this class.
@@ -34,20 +36,20 @@ public class PropDefId implements Serializable {
 
     /**
      * The internal id is computed based on the characteristics of the
     * <code>PropDef</code> that this <code>PropDefId</code> identifies.
     * <code>QPropertyDefinition</code> that this <code>PropDefId</code> identifies.
      */
     private final int id;
 
     /**
      * Creates a new <code>PropDefId</code> that serves as identifier for
     * the given <code>PropDef</code>. An internal id is computed based on
     * the characteristics of the <code>PropDef</code> that it identifies.
     * the given <code>QPropertyDefinition</code>. An internal id is computed based on
     * the characteristics of the <code>QPropertyDefinition</code> that it identifies.
      *
     * @param def <code>PropDef</code> to create identifier for
     * @param def <code>QPropertyDefinition</code> to create identifier for
      */
    PropDefId(PropDef def) {
    public PropDefId(QPropertyDefinition def) {
         if (def == null) {
            throw new IllegalArgumentException("PropDef argument can not be null");
            throw new IllegalArgumentException("QPropertyDefinition argument can not be null");
         }
         // build key (format: <declaringNodeType>/<name>/<requiredType>/<multiple>)
         StringBuffer sb = new StringBuffer();
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/PropDefImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/PropDefImpl.java
deleted file mode 100644
index a0821655e..000000000
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/PropDefImpl.java
++ /dev/null
@@ -1,388 +0,0 @@
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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.query.qom.Operator;
import org.apache.jackrabbit.spi.commons.QPropertyDefinitionImpl;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import java.util.Arrays;

/**
 * This class implements the <code>PropDef</code> interface and additionally
 * provides setter methods for the various property definition attributes.
 */
public class PropDefImpl extends ItemDefImpl implements PropDef {

    /**
     * The required type.
     */
    private int requiredType = PropertyType.UNDEFINED;

    /**
     * The value constraints.
     */
    private QValueConstraint[] valueConstraints = QValueConstraint.EMPTY_ARRAY;

    /**
     * The default values.
     */
    private InternalValue[] defaultValues = InternalValue.EMPTY_ARRAY;

    /**
     * The 'multiple' flag
     */
    private boolean multiple = false;

    /**
     * The identifier of this property definition. The identifier is lazily
     * computed based on the characteristics of this property definition and
     * reset on every attribute change.
     */
    private PropDefId id = null;

    /*
     * The 'fulltext searchable' flag.
     */
    private boolean fullTextSearchable = true;

    /*
     * The 'query orderable' flag.
     */
    private boolean queryOrderable = true;

    /*
     * The 'query operators.
     */
    private String[] queryOperators = Operator.getAllQueryOperators();


    /**
     * Default constructor.
     */
    public PropDefImpl() {
    }

    public PropDefImpl(QPropertyDefinition pd) {
        super(pd);
        requiredType = pd.getRequiredType();
        valueConstraints = pd.getValueConstraints();
        QValue[] vs = pd.getDefaultValues();
        if (vs != null) {
            defaultValues = new InternalValue[vs.length];
            for (int i=0; i<vs.length; i++) {
                try {
                    defaultValues[i] = InternalValue.create(vs[i]);
                } catch (RepositoryException e) {
                    throw new IllegalStateException("Error while converting default values.", e);
                }
            }
        }
        multiple = pd.isMultiple();
        fullTextSearchable = pd.isFullTextSearchable();
        queryOrderable = pd.isQueryOrderable();
        queryOperators = pd.getAvailableQueryOperators();
    }

    /**
     * Returns the QPropertyDefinition of this PropDef
     * @return the QPropertyDefinition
     */
    public QPropertyDefinition getQPropertyDefinition() {
        return new QPropertyDefinitionImpl(
                getName(),
                getDeclaringNodeType(),
                isAutoCreated(),
                isMandatory(),
                getOnParentVersion(),
                isProtected(),
                getDefaultValues(),
                isMultiple(),
                getRequiredType(),
                getValueConstraints(),
                getAvailableQueryOperators(),
                isFullTextSearchable(),
                isQueryOrderable()
        );
    }

    /**
     * Sets the required type
     *
     * @param requiredType
     */
    public void setRequiredType(int requiredType) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        this.requiredType = requiredType;
    }

    /**
     * Sets the value constraints.
     *
     * @param valueConstraints
     */
    public void setValueConstraints(QValueConstraint[] valueConstraints) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        if (valueConstraints != null) {
            this.valueConstraints = valueConstraints;
        } else {
            this.valueConstraints = QValueConstraint.EMPTY_ARRAY;
        }
    }

    /**
     * Sets the default values.
     *
     * @param defaultValues
     */
    public void setDefaultValues(InternalValue[] defaultValues) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        if (defaultValues != null) {
            this.defaultValues = defaultValues;
        } else {
            this.defaultValues = InternalValue.EMPTY_ARRAY;
        }
    }

    /**
     * Sets the 'multiple' flag.
     *
     * @param multiple
     */
    public void setMultiple(boolean multiple) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        this.multiple = multiple;
    }

    /**
     * Sets the 'fulltext searchable' flag.
     *
     * @param fullTextSearchable
     */
    public void setFullTextSearchable(boolean fullTextSearchable) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        this.fullTextSearchable = fullTextSearchable;
    }

    /**
     * Sets the 'fulltext searchable' flag.
     *
     * @param queryOrderable
     */
    public void setQueryOrderable(boolean queryOrderable) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        this.queryOrderable = queryOrderable;
    }

    /**
     * Sets the 'available' query operators.
     *
     * @param queryOperators
     */
    public void setAvailableQueryOperators(String[] queryOperators) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        if (queryOperators != null) {
            this.queryOperators = queryOperators;
        } else {
            this.queryOperators = new String[0];
        }
    }

    //------------------------------------------------< ItemDefImpl overrides >
    /**
     * {@inheritDoc}
     */
    public void setDeclaringNodeType(Name declaringNodeType) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        super.setDeclaringNodeType(declaringNodeType);
    }

    /**
     * {@inheritDoc}
     */
    public void setName(Name name) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        super.setName(name);
    }

    /**
     * {@inheritDoc}
     */
    public void setAutoCreated(boolean autoCreated) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        super.setAutoCreated(autoCreated);
    }

    /**
     * {@inheritDoc}
     */
    public void setOnParentVersion(int onParentVersion) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        super.setOnParentVersion(onParentVersion);
    }

    /**
     * {@inheritDoc}
     */
    public void setProtected(boolean writeProtected) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        super.setProtected(writeProtected);
    }

    /**
     * {@inheritDoc}
     */
    public void setMandatory(boolean mandatory) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        super.setMandatory(mandatory);
    }

    //--------------------------------------------------------------< PropDef >
    /**
     * {@inheritDoc}
     * <p/>
     * The identifier is computed based on the characteristics of this property
     * definition, i.e. modifying attributes of this property definition will
     * have impact on the identifier returned by this method.
     */
    public PropDefId getId() {
        if (id == null) {
            // generate new identifier based on this property definition
            id = new PropDefId(this);
        }
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public int getRequiredType() {
        return requiredType;
    }

    /**
     * {@inheritDoc}
     */
    public QValueConstraint[] getValueConstraints() {
        return valueConstraints;
    }

    /**
     * {@inheritDoc}
     */
    public InternalValue[] getDefaultValues() {
        return defaultValues;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMultiple() {
        return multiple;
    }

    /**
     * {@inheritDoc}
     *
     * @return always <code>false</code>
     */
    public boolean definesNode() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getAvailableQueryOperators() {
        return queryOperators;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFullTextSearchable() {
        return fullTextSearchable;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isQueryOrderable() {
        return queryOrderable;
    }

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * Compares two property definitions for equality. Returns <code>true</code>
     * if the given object is a property defintion and has the same attributes
     * as this property definition.
     *
     * @param obj the object to compare this property definition with
     * @return <code>true</code> if the object is equal to this property definition,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PropDefImpl) {
            PropDefImpl other = (PropDefImpl) obj;
            return super.equals(obj)
                    && requiredType == other.requiredType
                    && Arrays.equals(valueConstraints, other.valueConstraints)
                    && Arrays.equals(defaultValues, other.defaultValues)
                    && multiple == other.multiple
                    && Arrays.equals(queryOperators, other.queryOperators)
                    && queryOrderable == other.queryOrderable
                    && fullTextSearchable == other.fullTextSearchable;
        }
        return false;
    }

    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }

}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/PropertyDefinitionImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/PropertyDefinitionImpl.java
deleted file mode 100644
index 4f63e68d1..000000000
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/PropertyDefinitionImpl.java
++ /dev/null
@@ -1,152 +0,0 @@
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
package org.apache.jackrabbit.core.nodetype;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.spi.commons.nodetype.constraint.ValueConstraint;
import org.apache.jackrabbit.spi.commons.nodetype.InvalidConstraintException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the <code>PropertyDefinition</code> interface.
 * All method calls are delegated to the wrapped {@link PropDef},
 * performing the translation from <code>Name</code>s to JCR names
 * (and vice versa) where necessary.
 */
public class PropertyDefinitionImpl extends ItemDefinitionImpl
        implements PropertyDefinition {

    /**
     * Logger instance for this class
     */
    private static Logger log = LoggerFactory.getLogger(PropertyDefinitionImpl.class);

    private final ValueFactory valueFactory;

    /**
     * Package private constructor
     *
     * @param propDef    property definition
     * @param ntMgr      node type manager
     * @param resolver   name resolver
     * @param valueFactory the value factory
     */
    PropertyDefinitionImpl(PropDef propDef, NodeTypeManagerImpl ntMgr,
                           NamePathResolver resolver, ValueFactory valueFactory) {
        super(propDef, ntMgr, resolver);
        this.valueFactory = valueFactory;
    }

    /**
     * Returns the wrapped property definition.
     *
     * @return the wrapped property definition.
     */
    public PropDef unwrap() {
        return (PropDef) itemDef;
    }

    //---------------------------------------------------< PropertyDefinition >
    /**
     * {@inheritDoc}
     */
    public Value[] getDefaultValues() {
        InternalValue[] defVals = ((PropDef) itemDef).getDefaultValues();
        if (defVals == null) {
            return null;
        }
        Value[] values = new Value[defVals.length];
        for (int i = 0; i < defVals.length; i++) {
            try {
                values[i] = ValueFormat.getJCRValue(defVals[i], resolver, valueFactory);
            } catch (RepositoryException re) {
                // should never get here
                String propName = (getName() == null) ? "[null]" : getName();
                log.error("illegal default value specified for property "
                        + propName + " in node type " + getDeclaringNodeType(),
                        re);
                return null;
            }
        }
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public int getRequiredType() {
        return ((PropDef) itemDef).getRequiredType();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getValueConstraints() {
        QValueConstraint[] constraints = ((PropDef) itemDef).getValueConstraints();
        if (constraints == null || constraints.length == 0) {
            return new String[0];
        }
        String[] vca = new String[constraints.length];
        for (int i = 0; i < constraints.length; i++) {
            try {
                ValueConstraint vc = ValueConstraint.create(((PropDef) itemDef).getRequiredType(), constraints[i].getString());
                vca[i] = vc.getDefinition(resolver);
            } catch (InvalidConstraintException e) {
                log.warn("Error during conversion of value constraint.", e);
                vca[i] = constraints[i].getString();
            }
        }
        return vca;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMultiple() {
        return ((PropDef) itemDef).isMultiple();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getAvailableQueryOperators() {
        return ((PropDef) itemDef).getAvailableQueryOperators();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFullTextSearchable() {
        return ((PropDef) itemDef).isFullTextSearchable();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isQueryOrderable() {
        return ((PropDef) itemDef).isQueryOrderable();
    }
}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/virtual/VirtualNodeTypeStateProvider.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/virtual/VirtualNodeTypeStateProvider.java
index f6014723b..e8a16ebcc 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/virtual/VirtualNodeTypeStateProvider.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/virtual/VirtualNodeTypeStateProvider.java
@@ -25,11 +25,8 @@ import javax.jcr.RepositoryException;
 import javax.jcr.version.OnParentVersionAction;
 
 import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
 import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
 import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
 import org.apache.jackrabbit.core.state.ChangeLog;
 import org.apache.jackrabbit.core.state.ItemStateException;
 import org.apache.jackrabbit.core.state.NoSuchItemStateException;
@@ -38,6 +35,8 @@ import org.apache.jackrabbit.core.virtual.AbstractVISProvider;
 import org.apache.jackrabbit.core.virtual.VirtualNodeState;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
 
 /**
@@ -74,9 +73,6 @@ public class VirtualNodeTypeStateProvider extends AbstractVISProvider {
      */
     protected VirtualNodeState createRootNodeState() throws RepositoryException {
         VirtualNodeState root = new VirtualNodeState(this, parentId, rootNodeId, NameConstants.REP_NODETYPES, null);
        NodeDefId id = ntReg.getEffectiveNodeType(NameConstants.REP_SYSTEM).getApplicableChildNodeDef(
                NameConstants.JCR_NODETYPES, NameConstants.REP_NODETYPES, ntReg).getId();
        root.setDefinitionId(id);
         Name[] ntNames = ntReg.getRegisteredNodeTypes();
         for (int i = 0; i < ntNames.length; i++) {
             NodeTypeDef ntDef = ntReg.getNodeTypeDef(ntNames[i]);
@@ -168,7 +164,7 @@ public class VirtualNodeTypeStateProvider extends AbstractVISProvider {
         }
 
         // add property defs
        PropDef[] propDefs = ntDef.getPropertyDefs();
        QPropertyDefinition[] propDefs = ntDef.getPropertyDefs();
         for (int i = 0; i < propDefs.length; i++) {
             VirtualNodeState pdState = createPropertyDefState(ntState, propDefs[i], ntDef, i);
             ntState.addChildNodeEntry(NameConstants.JCR_PROPERTYDEFINITION, pdState.getNodeId());
@@ -177,7 +173,7 @@ public class VirtualNodeTypeStateProvider extends AbstractVISProvider {
         }
 
         // add child node defs
        NodeDef[] cnDefs = ntDef.getChildNodeDefs();
        QNodeDefinition[] cnDefs = ntDef.getChildNodeDefs();
         for (int i = 0; i < cnDefs.length; i++) {
             VirtualNodeState cnState = createChildNodeDefState(ntState, cnDefs[i], ntDef, i);
             ntState.addChildNodeEntry(NameConstants.JCR_CHILDNODEDEFINITION, cnState.getNodeId());
@@ -197,7 +193,7 @@ public class VirtualNodeTypeStateProvider extends AbstractVISProvider {
      * @throws RepositoryException
      */
     private VirtualNodeState createPropertyDefState(VirtualNodeState parent,
                                                    PropDef propDef,
                                                    QPropertyDefinition propDef,
                                                     NodeTypeDef ntDef, int n)
             throws RepositoryException {
         NodeId id = calculateStableId(
@@ -218,7 +214,7 @@ public class VirtualNodeTypeStateProvider extends AbstractVISProvider {
         pState.setPropertyValue(
                 NameConstants.JCR_REQUIREDTYPE,
                 InternalValue.create(PropertyType.nameFromValue(propDef.getRequiredType()).toUpperCase()));
        InternalValue[] defVals = propDef.getDefaultValues();
        InternalValue[] defVals = InternalValue.create(propDef.getDefaultValues());
         // retrieve the property type from the first default value present with
         // the property definition. in case no default values are defined,
         // fallback to PropertyType.STRING in order to avoid creating a property
@@ -227,7 +223,9 @@ public class VirtualNodeTypeStateProvider extends AbstractVISProvider {
         if (defVals != null && defVals.length > 0) {
             defValsType = defVals[0].getType();
         }
        pState.setPropertyValues(NameConstants.JCR_DEFAULTVALUES, defValsType, defVals);
        if (defVals != null) {
            pState.setPropertyValues(NameConstants.JCR_DEFAULTVALUES, defValsType, defVals);
        }
         QValueConstraint[] vc = propDef.getValueConstraints();
         InternalValue[] vals = new InternalValue[vc.length];
         for (int i = 0; i < vc.length; i++) {
@@ -246,7 +244,7 @@ public class VirtualNodeTypeStateProvider extends AbstractVISProvider {
      * @throws RepositoryException
      */
     private VirtualNodeState createChildNodeDefState(VirtualNodeState parent,
                                                     NodeDef cnDef,
                                                     QNodeDefinition cnDef,
                                                      NodeTypeDef ntDef, int n)
             throws RepositoryException {
         NodeId id = calculateStableId(
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/xml/NodeTypeReader.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/xml/NodeTypeReader.java
index ba9dc0185..481e62f3e 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/xml/NodeTypeReader.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/xml/NodeTypeReader.java
@@ -17,12 +17,7 @@
 package org.apache.jackrabbit.core.nodetype.xml;
 
 import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.ItemDef;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeDefImpl;
 import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropDefImpl;
 import org.apache.jackrabbit.core.util.DOMWalker;
 import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.core.value.InternalValueFactory;
@@ -34,9 +29,14 @@ import org.apache.jackrabbit.spi.commons.value.ValueFactoryQImpl;
 import org.apache.jackrabbit.spi.commons.value.ValueFormat;
 import org.apache.jackrabbit.spi.commons.nodetype.constraint.ValueConstraint;
 import org.apache.jackrabbit.spi.commons.nodetype.InvalidConstraintException;
import org.apache.jackrabbit.spi.commons.nodetype.QNodeDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.QPropertyDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.QValueFactory;
 import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
 import org.apache.jackrabbit.value.ValueHelper;
 
 import javax.jcr.PropertyType;
@@ -183,22 +183,22 @@ public class NodeTypeReader {
         }
 
         // property definitions
        List<PropDef> properties = new ArrayList<PropDef>();
        List<QPropertyDefinition> properties = new ArrayList<QPropertyDefinition>();
         while (walker.iterateElements(Constants.PROPERTYDEFINITION_ELEMENT)) {
            PropDefImpl def = getPropDef();
            QPropertyDefinitionBuilder def = getPropDef();
             def.setDeclaringNodeType(type.getName());
            properties.add(def);
            properties.add(def.build());
         }
        type.setPropertyDefs(properties.toArray(new PropDef[properties.size()]));
        type.setPropertyDefs(properties.toArray(new QPropertyDefinition[properties.size()]));
 
         // child node definitions
        List<NodeDef> nodes = new ArrayList<NodeDef>();
        List<QNodeDefinition> nodes = new ArrayList<QNodeDefinition>();
         while (walker.iterateElements(Constants.CHILDNODEDEFINITION_ELEMENT)) {
            NodeDefImpl def = getChildNodeDef();
            QNodeDefinitionBuilder def = getChildNodeDef();
             def.setDeclaringNodeType(type.getName());
            nodes.add(def);
            nodes.add(def.build());
         }
        type.setChildNodeDefs(nodes.toArray(new NodeDef[nodes.size()]));
        type.setChildNodeDefs(nodes.toArray(new QNodeDefinition[nodes.size()]));
 
         return type;
     }
@@ -212,26 +212,23 @@ public class NodeTypeReader {
      *                                     illegal name
      * @throws NamespaceException if a namespace is not defined
      */
    private PropDefImpl getPropDef()
    private QPropertyDefinitionBuilder getPropDef()
             throws InvalidNodeTypeDefException, NameException, NamespaceException {
        PropDefImpl def = new PropDefImpl();
        QPropertyDefinitionBuilder def = new QPropertyDefinitionBuilder();
         String name = walker.getAttribute(Constants.NAME_ATTRIBUTE);
         if (name.equals("*")) {
            def.setName(ItemDef.ANY_NAME);
            def.setName(NameConstants.ANY_NAME);
         } else {
             def.setName(resolver.getQName(name));
         }
 
         // simple attributes
         def.setAutoCreated(Boolean.valueOf(
                walker.getAttribute(Constants.AUTOCREATED_ATTRIBUTE))
                .booleanValue());
                walker.getAttribute(Constants.AUTOCREATED_ATTRIBUTE)));
         def.setMandatory(Boolean.valueOf(
                walker.getAttribute(Constants.MANDATORY_ATTRIBUTE))
                .booleanValue());
                walker.getAttribute(Constants.MANDATORY_ATTRIBUTE)));
         def.setProtected(Boolean.valueOf(
                walker.getAttribute(Constants.PROTECTED_ATTRIBUTE))
                .booleanValue());
                walker.getAttribute(Constants.PROTECTED_ATTRIBUTE)));
         def.setOnParentVersion(OnParentVersionAction.valueFromName(
                 walker.getAttribute(Constants.ONPARENTVERSION_ATTRIBUTE)));
         def.setMultiple(Boolean.valueOf(
@@ -320,25 +317,22 @@ public class NodeTypeReader {
      * @throws NameException if the definition contains an illegal name
      * @throws NamespaceException if a namespace is not defined
      */
    private NodeDefImpl getChildNodeDef() throws NameException, NamespaceException {
        NodeDefImpl def = new NodeDefImpl();
    private QNodeDefinitionBuilder getChildNodeDef() throws NameException, NamespaceException {
        QNodeDefinitionBuilder def = new QNodeDefinitionBuilder();
         String name = walker.getAttribute(Constants.NAME_ATTRIBUTE);
         if (name.equals("*")) {
            def.setName(ItemDef.ANY_NAME);
            def.setName(NameConstants.ANY_NAME);
         } else {
             def.setName(resolver.getQName(name));
         }
 
         // simple attributes
         def.setAutoCreated(Boolean.valueOf(
                walker.getAttribute(Constants.AUTOCREATED_ATTRIBUTE))
                .booleanValue());
                walker.getAttribute(Constants.AUTOCREATED_ATTRIBUTE)));
         def.setMandatory(Boolean.valueOf(
                walker.getAttribute(Constants.MANDATORY_ATTRIBUTE))
                .booleanValue());
                walker.getAttribute(Constants.MANDATORY_ATTRIBUTE)));
         def.setProtected(Boolean.valueOf(
                walker.getAttribute(Constants.PROTECTED_ATTRIBUTE))
                .booleanValue());
                walker.getAttribute(Constants.PROTECTED_ATTRIBUTE)));
         def.setOnParentVersion(OnParentVersionAction.valueFromName(
                 walker.getAttribute(Constants.ONPARENTVERSION_ATTRIBUTE)));
         def.setAllowsSameNameSiblings(Boolean.valueOf(
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/xml/NodeTypeWriter.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/xml/NodeTypeWriter.java
index 4c56e66cf..64f18f6e1 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/xml/NodeTypeWriter.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/nodetype/xml/NodeTypeWriter.java
@@ -16,11 +16,8 @@
  */
 package org.apache.jackrabbit.core.nodetype.xml;
 
import org.apache.jackrabbit.core.nodetype.NodeDef;
 import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.PropDef;
 import org.apache.jackrabbit.core.util.DOMBuilder;
import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.core.value.InternalValueFactory;
 import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
 import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
@@ -30,6 +27,9 @@ import org.apache.jackrabbit.spi.commons.value.ValueFactoryQImpl;
 import org.apache.jackrabbit.spi.commons.nodetype.constraint.ValueConstraint;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
 
 import javax.jcr.NamespaceRegistry;
 import javax.jcr.PropertyType;
@@ -165,14 +165,14 @@ public final class NodeTypeWriter {
         }
 
         // property definitions
        PropDef[] properties = def.getPropertyDefs();
        for (PropDef property : properties) {
        QPropertyDefinition[] properties = def.getPropertyDefs();
        for (QPropertyDefinition property : properties) {
             addPropDef(property);
         }
 
         // child node definitions
        NodeDef[] nodes = def.getChildNodeDefs();
        for (NodeDef node : nodes) {
        QNodeDefinition[] nodes = def.getChildNodeDefs();
        for (QNodeDefinition node : nodes) {
             addChildNodeDef(node);
         }
 
@@ -188,7 +188,7 @@ public final class NodeTypeWriter {
      * @throws NamespaceException if the property definition contains
      *                                   invalid namespace references
      */
    private void addPropDef(PropDef def)
    private void addPropDef(QPropertyDefinition def)
             throws NamespaceException, RepositoryException {
         builder.startElement(Constants.PROPERTYDEFINITION_ELEMENT);
 
@@ -261,10 +261,10 @@ public final class NodeTypeWriter {
         }
 
         // default values
        InternalValue[] defaults = def.getDefaultValues();
        QValue[] defaults = def.getDefaultValues();
         if (defaults != null && defaults.length > 0) {
             builder.startElement(Constants.DEFAULTVALUES_ELEMENT);
            for (InternalValue v : defaults) {
            for (QValue v : defaults) {
                 builder.addContentElement(
                         Constants.DEFAULTVALUE_ELEMENT,
                         factory.createValue(v).getString());
@@ -282,7 +282,7 @@ public final class NodeTypeWriter {
      * @throws NamespaceException if the child node definition contains
      *                                   invalid namespace references
      */
    private void addChildNodeDef(NodeDef def)
    private void addChildNodeDef(QNodeDefinition def)
             throws NamespaceException {
         builder.startElement(Constants.CHILDNODEDEFINITION_ELEMENT);
 
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/PersistenceCopier.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/PersistenceCopier.java
index 6fc3a6dbf..152bf70f6 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/PersistenceCopier.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/PersistenceCopier.java
@@ -132,7 +132,6 @@ public class PersistenceCopier {
             // Copy the node state
             NodeState targetNode = target.createNew(sourceNode.getNodeId());
             targetNode.setParentId(sourceNode.getParentId());
            targetNode.setDefinitionId(sourceNode.getDefinitionId());
             targetNode.setNodeTypeName(sourceNode.getNodeTypeName());
             targetNode.setMixinTypeNames(sourceNode.getMixinTypeNames());
             targetNode.setPropertyNames(sourceNode.getPropertyNames());
@@ -148,7 +147,6 @@ public class PersistenceCopier {
                 PropertyId id = new PropertyId(sourceNode.getNodeId(), name);
                 PropertyState sourceState = source.load(id);
                 PropertyState targetState = target.createNew(id);
                targetState.setDefinitionId(sourceState.getDefinitionId());
                 targetState.setType(sourceState.getType());
                 targetState.setMultiValued(sourceState.isMultiValued());
                 InternalValue[] values = sourceState.getValues();
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/AbstractBundlePersistenceManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/AbstractBundlePersistenceManager.java
index cbad1d006..8559cee2d 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/AbstractBundlePersistenceManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/AbstractBundlePersistenceManager.java
@@ -31,7 +31,6 @@ import org.apache.jackrabbit.core.id.ItemId;
 import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.id.PropertyId;
 import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.nodetype.PropDefId;
 import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.core.persistence.IterablePersistenceManager;
 import org.apache.jackrabbit.core.persistence.PMContext;
@@ -109,15 +108,6 @@ public abstract class AbstractBundlePersistenceManager implements
     /** the cache of non-existent bundles */
     private LRUNodeIdCache missing;
 
    /** definition id of the jcr:uuid property */
    private PropDefId idJcrUUID;

    /** definition id of the jcr:primaryType property */
    private PropDefId idJcrPrimaryType;

    /** definition id of the jcr:mixinTypes property */
    private PropDefId idJcrMixinTypes;

     /** the persistence manager context */
     protected PMContext context;
 
@@ -397,18 +387,6 @@ public abstract class AbstractBundlePersistenceManager implements
         // init bundle cache
         bundles = new BundleCache(bundleCacheSize);
         missing = new LRUNodeIdCache();

        // init property definitions
        if (context.getNodeTypeRegistry() != null) {
            idJcrUUID = context.getNodeTypeRegistry()
                .getEffectiveNodeType(NameConstants.MIX_REFERENCEABLE)
                .getApplicablePropertyDef(NameConstants.JCR_UUID, PropertyType.STRING, false)
                .getId();
            idJcrPrimaryType = context.getNodeTypeRegistry().getEffectiveNodeType(NameConstants.NT_BASE).getApplicablePropertyDef(
                    NameConstants.JCR_PRIMARYTYPE, PropertyType.NAME, false).getId();
            idJcrMixinTypes = context.getNodeTypeRegistry().getEffectiveNodeType(NameConstants.NT_BASE).getApplicablePropertyDef(
                    NameConstants.JCR_MIXINTYPES, PropertyType.NAME, true).getId();
        }
     }
     
     /**
@@ -453,22 +431,19 @@ public abstract class AbstractBundlePersistenceManager implements
             if (id.getName().equals(NameConstants.JCR_UUID)) {
                 state = createNew(id);
                 state.setType(PropertyType.STRING);
                state.setDefinitionId(idJcrUUID);
                 state.setMultiValued(false);
                 state.setValues(new InternalValue[]{InternalValue.create(id.getParentId().toString())});
             } else if (id.getName().equals(NameConstants.JCR_PRIMARYTYPE)) {
                 state = createNew(id);
                 state.setType(PropertyType.NAME);
                state.setDefinitionId(idJcrPrimaryType);
                 state.setMultiValued(false);
                 state.setValues(new InternalValue[]{InternalValue.create(bundle.getNodeTypeName())});
             } else if (id.getName().equals(NameConstants.JCR_MIXINTYPES)) {
                 Set<Name> mixins = bundle.getMixinTypeNames();
                 state = createNew(id);
                 state.setType(PropertyType.NAME);
                state.setDefinitionId(idJcrMixinTypes);
                 state.setMultiValued(true);
                state.setValues(InternalValue.create((Name[]) mixins.toArray(new Name[mixins.size()])));
                state.setValues(InternalValue.create(mixins.toArray(new Name[mixins.size()])));
             } else {
                 throw new NoSuchItemStateException(id.toString());
             }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/BundleBinding.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/BundleBinding.java
index 76e142578..711ff05a5 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/BundleBinding.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/BundleBinding.java
@@ -26,8 +26,6 @@ import org.apache.jackrabbit.core.id.PropertyId;
 import org.apache.jackrabbit.core.util.StringIndex;
 import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.PropDefId;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
@@ -100,7 +98,7 @@ public class BundleBinding extends ItemStateBinding {
         bundle.setParentId(readID(in));
 
         // definitionId
        bundle.setNodeDefId(NodeDefId.valueOf(in.readUTF()));
        in.readUTF();
 
         // mixin types
         Set<Name> mixinTypeNames = new HashSet<Name>();
@@ -273,7 +271,7 @@ public class BundleBinding extends ItemStateBinding {
         writeID(out, bundle.getParentId());
 
         // definitionId
        out.writeUTF(bundle.getNodeDefId().toString());
        out.writeUTF("");
 
         // mixin types
         for (Name name : bundle.getMixinTypeNames()) {
@@ -342,7 +340,7 @@ public class BundleBinding extends ItemStateBinding {
         // multiValued
         entry.setMultiValued(in.readBoolean());
         // definitionId
        entry.setPropDefId(PropDefId.valueOf(in.readUTF()));
        in.readUTF();
         // values
         int count = in.readInt();   // count
         InternalValue[] values = new InternalValue[count];
@@ -598,7 +596,7 @@ public class BundleBinding extends ItemStateBinding {
         // multiValued
         out.writeBoolean(state.isMultiValued());
         // definitionId
        out.writeUTF(state.getPropDefId().toString());
        out.writeUTF("");
         // values
         InternalValue[] values = state.getValues();
         out.writeInt(values.length); // count
@@ -736,7 +734,7 @@ public class BundleBinding extends ItemStateBinding {
      * Write a small binary value and return the data.
      *
      * @param out the output stream to write
     * @param blobVal the binary value
     * @param value the binary value
      * @param state the property state (for error messages)
      * @param i the index (for error messages)
      * @return the data
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/ItemStateBinding.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/ItemStateBinding.java
index 7b9f7ce88..87e2e2536 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/ItemStateBinding.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/ItemStateBinding.java
@@ -24,7 +24,6 @@ import org.apache.jackrabbit.core.util.StringIndex;
 import org.apache.jackrabbit.core.id.PropertyId;
 import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
 
@@ -158,7 +157,7 @@ public class ItemStateBinding {
         // parentUUID
         state.setParentId(readID(in));
         // definitionId
        state.setDefinitionId(NodeDefId.valueOf(in.readUTF()));
        in.readUTF();
 
         // mixin types
         int count = in.readInt();   // count
@@ -210,7 +209,7 @@ public class ItemStateBinding {
         // parentUUID
         writeID(out, state.getParentId());
         // definitionId
        out.writeUTF(state.getDefinitionId().toString());
        out.writeUTF("");
         // mixin types
         Collection<Name> c = state.getMixinTypeNames();
         out.writeInt(c.size()); // count
@@ -261,7 +260,7 @@ public class ItemStateBinding {
     /**
      * Serializes a node identifier
      * @param out the output stream
     * @param uuid the node id
     * @param id the node id
      * @throws IOException in an I/O error occurs.
      */
     public void writeNodeId(DataOutputStream out, String id) throws IOException {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/NodePropBundle.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/NodePropBundle.java
index f45d01d16..281d22216 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/NodePropBundle.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/bundle/util/NodePropBundle.java
@@ -32,8 +32,6 @@ import org.apache.jackrabbit.core.persistence.util.BLOBStore;
 import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.core.state.PropertyState;
 import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.PropDefId;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
 import org.slf4j.Logger;
@@ -75,11 +73,6 @@ public class NodePropBundle {
      */
     private Set<Name> mixinTypeNames;
 
    /**
     * the nodedef id
     */
    private NodeDefId nodeDefId;

     /**
      * the child node entries
      */
@@ -148,7 +141,6 @@ public class NodePropBundle {
         parentId = state.getParentId();
         nodeTypeName = state.getNodeTypeName();
         mixinTypeNames = state.getMixinTypeNames();
        nodeDefId = state.getDefinitionId();
         isReferenceable = state.hasPropertyName(NameConstants.JCR_UUID);
         modCount = state.getModCount();
         List<org.apache.jackrabbit.core.state.ChildNodeEntry> list = state.getChildNodeEntries();
@@ -169,7 +161,6 @@ public class NodePropBundle {
         state.setParentId(parentId);
         state.setNodeTypeName(nodeTypeName);
         state.setMixinTypeNames(mixinTypeNames);
        state.setDefinitionId(nodeDefId);
         state.setModCount(modCount);
         for (ChildNodeEntry e : childNodeEntries) {
             state.addChildNodeEntry(e.getName(), e.getId());
@@ -203,7 +194,6 @@ public class NodePropBundle {
             return null;
         }
         PropertyState ps = pMgr.createNew(new PropertyId(id, name));
        ps.setDefinitionId(p.getPropDefId());
         ps.setMultiValued(p.isMultiValued());
         ps.setType(p.getType());
         ps.setValues(p.getValues());
@@ -283,22 +273,6 @@ public class NodePropBundle {
         this.mixinTypeNames = mixinTypeNames;
     }
 
    /**
     * Returns the node def id of this bundle.
     * @return the node def id.
     */
    public NodeDefId getNodeDefId() {
        return nodeDefId;
    }

    /**
     * Sets the node def id.
     * @param nodeDefId the node def id.
     */
    public void setNodeDefId(NodeDefId nodeDefId) {
        this.nodeDefId = nodeDefId;
    }

     /**
      * Checks if this bundle is referenceable.
      * @return <code>true</code> if this bundle is referenceable;
@@ -548,11 +522,6 @@ public class NodePropBundle {
          */
         private boolean multiValued;
 
        /**
         * the propedef id
         */
        private PropDefId propDefId;

         /**
          * the blob ids
          */
@@ -581,7 +550,6 @@ public class NodePropBundle {
             values = state.getValues();
             type = state.getType();
             multiValued = state.isMultiValued();
            propDefId = state.getDefinitionId();
             modCount = state.getModCount();
             if (type == PropertyType.BINARY) {
                 blobIds = new String[values.length];
@@ -652,22 +620,6 @@ public class NodePropBundle {
             this.multiValued = multiValued;
         }
 
        /**
         * Returns the propdef id.
         * @return the propdef id.
         */
        public PropDefId getPropDefId() {
            return propDefId;
        }

        /**
         * Sets the propdef id
         * @param propDefId the propdef id
         */
        public void setPropDefId(PropDefId propDefId) {
            this.propDefId = propDefId;
        }

         /**
          * Returns the n<sup>th</sup> blob id.
          * @param n the index of the blob id
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/util/Serializer.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/util/Serializer.java
index e707365aa..2b1975593 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/util/Serializer.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/util/Serializer.java
@@ -20,8 +20,6 @@ import org.apache.commons.io.IOUtils;
 import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.id.PropertyId;
 import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.PropDefId;
 import org.apache.jackrabbit.core.state.NodeReferences;
 import org.apache.jackrabbit.core.state.NodeState;
 import org.apache.jackrabbit.core.state.PropertyState;
@@ -82,7 +80,7 @@ public final class Serializer {
             out.write(state.getParentId().getRawBytes());
         }
         // definitionId
        out.writeUTF(state.getDefinitionId().toString());
        out.writeUTF("");
         // mixin types
         Collection<Name> c = state.getMixinTypeNames();
         out.writeInt(c.size()); // count
@@ -131,8 +129,7 @@ public final class Serializer {
             state.setParentId(new NodeId(uuidBytes));
         }
         // definitionId
        s = in.readUTF();
        state.setDefinitionId(NodeDefId.valueOf(s));
        in.readUTF();
         // mixin types
         int count = in.readInt();   // count
         Set<Name> set = new HashSet<Name>(count);
@@ -183,7 +180,7 @@ public final class Serializer {
         // multiValued
         out.writeBoolean(state.isMultiValued());
         // definitionId
        out.writeUTF(state.getDefinitionId().toString());
        out.writeUTF("");
         // modCount
         out.writeShort(state.getModCount());
         // values
@@ -259,8 +256,7 @@ public final class Serializer {
         boolean multiValued = in.readBoolean();
         state.setMultiValued(multiValued);
         // definitionId
        String s = in.readUTF();
        state.setDefinitionId(PropDefId.valueOf(s));
        in.readUTF();
         // modCount
         short modCount = in.readShort();
         state.setModCount(modCount);
@@ -270,7 +266,7 @@ public final class Serializer {
         for (int i = 0; i < count; i++) {
             InternalValue val;
             if (type == PropertyType.BINARY) {
                s = in.readUTF();   // value (i.e. blobId)
                String s = in.readUTF();   // value (i.e. blobId)
                 // special handling required for binary value:
                 // the value stores the id of the BLOB data
                 // in the BLOB store
@@ -302,7 +298,7 @@ public final class Serializer {
                 int len = in.readInt(); // lenght of byte[]
                 byte[] bytes = new byte[len];
                 in.readFully(bytes); // byte[]
                s = new String(bytes, ENCODING);
                String s = new String(bytes, ENCODING);
                 val = InternalValue.valueOf(s, type);
             }
             values[i] = val;
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/xml/XMLPersistenceManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/xml/XMLPersistenceManager.java
index 128267a2e..9bb0cc651 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/xml/XMLPersistenceManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/persistence/xml/XMLPersistenceManager.java
@@ -24,8 +24,6 @@ import org.apache.jackrabbit.core.fs.FileSystem;
 import org.apache.jackrabbit.core.fs.FileSystemException;
 import org.apache.jackrabbit.core.fs.FileSystemResource;
 import org.apache.jackrabbit.core.fs.local.LocalFileSystem;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.PropDefId;
 import org.apache.jackrabbit.core.persistence.AbstractPersistenceManager;
 import org.apache.jackrabbit.core.state.ItemStateException;
 import org.apache.jackrabbit.core.state.NoSuchItemStateException;
@@ -87,7 +85,6 @@ public class XMLPersistenceManager extends AbstractPersistenceManager {
     private static final String UUID_ATTRIBUTE = "uuid";
     private static final String NODETYPE_ATTRIBUTE = "nodeType";
     private static final String PARENTUUID_ATTRIBUTE = "parentUUID";
    private static final String DEFINITIONID_ATTRIBUTE = "definitionId";
     private static final String MODCOUNT_ATTRIBUTE = "modCount";
 
     private static final String MIXINTYPES_ELEMENT = "mixinTypes";
@@ -243,10 +240,6 @@ public class XMLPersistenceManager extends AbstractPersistenceManager {
             state.setParentId(NodeId.valueOf(parentUUID));
         }
 
        // definition id
        String definitionId = walker.getAttribute(DEFINITIONID_ATTRIBUTE);
        state.setDefinitionId(NodeDefId.valueOf(definitionId));

         // modification count
         String modCount = walker.getAttribute(MODCOUNT_ATTRIBUTE);
         state.setModCount(Short.parseShort(modCount));
@@ -324,10 +317,6 @@ public class XMLPersistenceManager extends AbstractPersistenceManager {
         String multiValued = walker.getAttribute(MULTIVALUED_ATTRIBUTE);
         state.setMultiValued(Boolean.getBoolean(multiValued));
 
        // definition id
        String definitionId = walker.getAttribute(DEFINITIONID_ATTRIBUTE);
        state.setDefinitionId(PropDefId.valueOf(definitionId));

         // modification count
         String modCount = walker.getAttribute(MODCOUNT_ATTRIBUTE);
         state.setModCount(Short.parseShort(modCount));
@@ -567,7 +556,6 @@ public class XMLPersistenceManager extends AbstractPersistenceManager {
                 writer.write("<" + NODE_ELEMENT + " "
                         + UUID_ATTRIBUTE + "=\"" + id + "\" "
                         + PARENTUUID_ATTRIBUTE + "=\"" + parentId + "\" "
                        + DEFINITIONID_ATTRIBUTE + "=\"" + state.getDefinitionId() + "\" "
                         + MODCOUNT_ATTRIBUTE + "=\"" + state.getModCount() + "\" "
                         + NODETYPE_ATTRIBUTE + "=\"" + encodedNodeType + "\">\n");
 
@@ -650,7 +638,6 @@ public class XMLPersistenceManager extends AbstractPersistenceManager {
                         + NAME_ATTRIBUTE + "=\"" + Text.encodeIllegalXMLCharacters(state.getName().toString()) + "\" "
                         + PARENTUUID_ATTRIBUTE + "=\"" + state.getParentId() + "\" "
                         + MULTIVALUED_ATTRIBUTE + "=\"" + Boolean.toString(state.isMultiValued()) + "\" "
                        + DEFINITIONID_ATTRIBUTE + "=\"" + state.getDefinitionId().toString() + "\" "
                         + MODCOUNT_ATTRIBUTE + "=\"" + state.getModCount() + "\" "
                         + TYPE_ATTRIBUTE + "=\"" + typeName + "\">\n");
                 // values
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/PropertyTypeRegistry.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/PropertyTypeRegistry.java
index b6b324ce4..c76276bfe 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/PropertyTypeRegistry.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/PropertyTypeRegistry.java
@@ -19,8 +19,8 @@ package org.apache.jackrabbit.core.query;
 import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
 import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
 import org.apache.jackrabbit.core.nodetype.NodeTypeRegistryListener;
import org.apache.jackrabbit.core.nodetype.PropDef;
 import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -87,9 +87,9 @@ public class PropertyTypeRegistry implements NodeTypeRegistryListener {
     public void nodeTypeRegistered(Name ntName) {
         try {
             NodeTypeDef def = registry.getNodeTypeDef(ntName);
            PropDef[] propDefs = def.getPropertyDefs();
            QPropertyDefinition[] propDefs = def.getPropertyDefs();
             synchronized (typeMapping) {
                for (PropDef propDef : propDefs) {
                for (QPropertyDefinition propDef : propDefs) {
                     int type = propDef.getRequiredType();
                     if (!propDef.definesResidual() && type != PropertyType.UNDEFINED) {
                         Name name = propDef.getName();
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/QueryImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/QueryImpl.java
index a3bfe8ed6..d64b05977 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/QueryImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/QueryImpl.java
@@ -28,10 +28,10 @@ import javax.jcr.query.qom.QueryObjectModelFactory;
 import org.apache.jackrabbit.core.ItemManager;
 import org.apache.jackrabbit.core.SessionImpl;
 import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.PropertyDefinitionImpl;
 import org.apache.jackrabbit.core.query.PropertyTypeRegistry;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QPropertyDefinition;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
 import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
 import org.apache.jackrabbit.spi.commons.query.AndQueryNode;
@@ -43,6 +43,7 @@ import org.apache.jackrabbit.spi.commons.query.QueryNodeFactory;
 import org.apache.jackrabbit.spi.commons.query.QueryParser;
 import org.apache.jackrabbit.spi.commons.query.QueryRootNode;
 import org.apache.jackrabbit.spi.commons.query.qom.ColumnImpl;
import org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl;
 import org.apache.lucene.search.Query;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -174,9 +175,9 @@ public class QueryImpl extends AbstractQueryImpl {
             NodeTypeImpl nt = session.getNodeTypeManager().getNodeType(ntName[0]);
             PropertyDefinition[] propDefs = nt.getPropertyDefinitions();
             for (PropertyDefinition pd : propDefs) {
                PropertyDefinitionImpl propDef = (PropertyDefinitionImpl) pd;
                QPropertyDefinition propDef = ((PropertyDefinitionImpl) pd).unwrap();
                 if (!propDef.definesResidual() && !propDef.isMultiple()) {
                    columns.put(propDef.getQName(), columnForName(propDef.getQName()));
                    columns.put(propDef.getName(), columnForName(propDef.getName()));
                 }
             }
         }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/QueryObjectModelImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/QueryObjectModelImpl.java
index 7c764bfab..832196cd7 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/QueryObjectModelImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/QueryObjectModelImpl.java
@@ -29,7 +29,6 @@ import org.apache.jackrabbit.core.ItemManager;
 import org.apache.jackrabbit.core.SessionImpl;
 import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
 import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.PropertyDefinitionImpl;
 import org.apache.jackrabbit.core.query.PropertyTypeRegistry;
 import org.apache.jackrabbit.core.query.lucene.constraint.Constraint;
 import org.apache.jackrabbit.core.query.lucene.constraint.ConstraintBuilder;
@@ -39,6 +38,7 @@ import org.apache.jackrabbit.spi.commons.query.qom.DefaultTraversingQOMTreeVisit
 import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelTree;
 import org.apache.jackrabbit.spi.commons.query.qom.SelectorImpl;
 import org.apache.jackrabbit.spi.commons.query.qom.OrderingImpl;
import org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl;
 
 /**
  * <code>QueryObjectModelImpl</code>...
@@ -123,7 +123,7 @@ public class QueryObjectModelImpl extends AbstractQueryImpl {
                 NodeTypeImpl nt = ntMgr.getNodeType(selector.getNodeTypeQName());
                 for (PropertyDefinition pd : nt.getPropertyDefinitions()) {
                     PropertyDefinitionImpl propDef = (PropertyDefinitionImpl) pd;
                    if (!propDef.definesResidual() && !propDef.isMultiple()) {
                    if (!propDef.unwrap().definesResidual() && !propDef.isMultiple()) {
                         String sn = selector.getSelectorName();
                         String pn = propDef.getName();
                         columns.add((ColumnImpl) qomFactory.column(sn, pn, sn + "." + pn));
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/NodeState.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/NodeState.java
index f0b6d0569..2fad12729 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/NodeState.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/NodeState.java
@@ -18,7 +18,6 @@ package org.apache.jackrabbit.core.state;
 
 import org.apache.jackrabbit.core.id.ItemId;
 import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
 import org.apache.jackrabbit.spi.Name;
 
 import java.util.ArrayList;
@@ -55,11 +54,6 @@ public class NodeState extends ItemState {
      */
     private NodeId parentId;
 
    /**
     * id of this node's definition
     */
    private NodeDefId defId;

     /**
      * insertion-ordered collection of ChildNodeEntry objects
      */
@@ -127,7 +121,6 @@ public class NodeState extends ItemState {
             parentId = nodeState.parentId;
             nodeTypeName = nodeState.nodeTypeName;
             mixinTypeNames = (NameSet) nodeState.mixinTypeNames.clone();
            defId = nodeState.defId;
             propertyNames = (NameSet) nodeState.propertyNames.clone();
             childNodeEntries = (ChildNodeEntries) nodeState.childNodeEntries.clone();
             if (syncModCount) {
@@ -210,24 +203,6 @@ public class NodeState extends ItemState {
         mixinTypeNames.replaceAll(names);
     }
 
    /**
     * Returns the id of the definition applicable to this node state.
     *
     * @return the id of the definition
     */
    public NodeDefId getDefinitionId() {
        return defId;
    }

    /**
     * Sets the id of the definition applicable to this node state.
     *
     * @param defId the id of the definition
     */
    public void setDefinitionId(NodeDefId defId) {
        this.defId = defId;
    }

     /**
      * Determines if there are any child node entries.
      *
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/PropertyState.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/PropertyState.java
index 569552a78..10627bae7 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/PropertyState.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/PropertyState.java
@@ -19,9 +19,9 @@ package org.apache.jackrabbit.core.state;
 import org.apache.jackrabbit.core.id.PropertyId;
 import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.nodetype.PropDefId;
 import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
 
 import javax.jcr.PropertyType;
 
@@ -51,9 +51,9 @@ public class PropertyState extends ItemState {
     private boolean multiValued;
 
     /**
     * the property definition id
     * the property definition
      */
    private PropDefId defId;
    private QPropertyDefinition def;
 
     /**
      * Constructs a new property state that is initially connected to an
@@ -92,7 +92,7 @@ public class PropertyState extends ItemState {
             PropertyState propState = (PropertyState) state;
             id = propState.id;
             type = propState.type;
            defId = propState.defId;
            def = propState.def;
             values = propState.values;
             multiValued = propState.multiValued;
             if (syncModCount) {
@@ -182,24 +182,6 @@ public class PropertyState extends ItemState {
         return multiValued;
     }
 
    /**
     * Returns the id of the definition applicable to this property state.
     *
     * @return the id of the definition
     */
    public PropDefId getDefinitionId() {
        return defId;
    }

    /**
     * Sets the id of the definition applicable to this property state.
     *
     * @param defId the id of the definition
     */
    public void setDefinitionId(PropDefId defId) {
        this.defId = defId;
    }

     /**
      * Sets the value(s) of this property.
      *
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java
index 5cf474a9f..808733fd6 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SessionItemStateManager.java
@@ -27,6 +27,7 @@ import javax.jcr.InvalidItemStateException;
 import javax.jcr.ItemNotFoundException;
 import javax.jcr.ReferentialIntegrityException;
 import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
 
 import org.apache.commons.collections.iterators.IteratorChain;
 import org.apache.jackrabbit.core.CachingHierarchyManager;
@@ -35,10 +36,12 @@ import org.apache.jackrabbit.core.ZombieHierarchyManager;
 import org.apache.jackrabbit.core.id.ItemId;
 import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.NodeDef;
 import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
 import org.apache.jackrabbit.core.util.Dumpable;
 import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -782,7 +785,7 @@ public class SessionItemStateManager
      *
      * @return attic
      */
    ItemStateManager getAttic() {
    public ItemStateManager getAttic() {
         if (attic == null) {
             attic = new AtticItemStateManager();
         }
@@ -861,14 +864,19 @@ public class SessionItemStateManager
                                 }
 
                                 public boolean allowsSameNameSiblings(NodeId id) {
                                    NodeState ns;
                                     try {
                                        ns = (NodeState) getItemState(id);
                                    } catch (ItemStateException e) {
                                        NodeState ns = (NodeState) getItemState(id);
                                        NodeState parent = (NodeState) getItemState(ns.getParentId());
                                        Name name = parent.getChildNodeEntry(id).getName();
                                        EffectiveNodeType ent = ntReg.getEffectiveNodeType(
                                                parent.getNodeTypeName(),
                                                parent.getMixinTypeNames());
                                        QNodeDefinition def = ent.getApplicableChildNodeDef(name, ns.getNodeTypeName(), ntReg);
                                        return def != null ? def.allowsSameNameSiblings() : false;
                                    } catch (Exception e) {
                                        log.warn("Unable to get node definition", e);
                                         return false;
                                     }
                                    NodeDef def = ntReg.getNodeDef(ns.getDefinitionId());
                                    return def != null ? def.allowsSameNameSiblings() : false;
                                 }
                             };
                     if (NodeStateMerger.merge((NodeState) transientState, context)) {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
index 870a54638..0f7e89699 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/state/SharedItemStateManager.java
@@ -24,7 +24,6 @@ import java.util.Set;
 import javax.jcr.PropertyType;
 import javax.jcr.ReferentialIntegrityException;
 import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
 import javax.jcr.nodetype.NoSuchNodeTypeException;
 
 import org.apache.jackrabbit.core.RepositoryImpl;
@@ -33,11 +32,8 @@ import org.apache.jackrabbit.core.id.ItemId;
 import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.id.PropertyId;
 import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
 import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
 import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
 import org.apache.jackrabbit.core.observation.EventState;
 import org.apache.jackrabbit.core.observation.EventStateCollection;
 import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
@@ -47,6 +43,7 @@ import org.apache.jackrabbit.core.util.Dumpable;
 import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;
 import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -621,18 +618,28 @@ public class SharedItemStateManager
                                         }
 
                                         public boolean allowsSameNameSiblings(NodeId id) {
                                            NodeState ns;
                                             try {
                                                if (local.has(id)) {
                                                    ns = (NodeState) local.get(id);
                                                } else {
                                                    ns = (NodeState) getItemState(id);
                                                }
                                            } catch (ItemStateException e) {
                                                NodeState ns = getNodeState(id);
                                                NodeState parent = getNodeState(ns.getParentId());
                                                Name name = parent.getChildNodeEntry(id).getName();
                                                EffectiveNodeType ent = ntReg.getEffectiveNodeType(
                                                        parent.getNodeTypeName(),
                                                        parent.getMixinTypeNames());
                                                QNodeDefinition def = ent.getApplicableChildNodeDef(name, ns.getNodeTypeName(), ntReg);
                                                return def != null ? def.allowsSameNameSiblings() : false;
                                            } catch (Exception e) {
                                                log.warn("Unable to get node definition", e);
                                                 return false;
                                             }
                                            NodeDef def = ntReg.getNodeDef(ns.getDefinitionId());
                                            return def != null ? def.allowsSameNameSiblings() : false;
                                        }

                                        protected NodeState getNodeState(NodeId id)
                                                throws ItemStateException {
                                            if (local.has(id)) {
                                                return (NodeState) local.get(id);
                                            } else {
                                                return (NodeState) getItemState(id);
                                            }
                                         }
                                     };
 
@@ -1248,47 +1255,21 @@ public class SharedItemStateManager
         // FIXME need to manually setup root node by creating mandatory jcr:primaryType property
         // @todo delegate setup of root node to NodeTypeInstanceHandler
 
        // id of the root node's definition
        NodeDefId nodeDefId;
        // definition of jcr:primaryType property
        PropDef propDef;
        // id of the jcr:system node's definition
        NodeDefId jcrSystemDefId;
        try {
            nodeDefId = ntReg.getRootNodeDef().getId();
            EffectiveNodeType ent = ntReg.getEffectiveNodeType(NameConstants.REP_ROOT);
            propDef = ent.getApplicablePropertyDef(NameConstants.JCR_PRIMARYTYPE,
                    PropertyType.NAME, false);
            jcrSystemDefId = ent.getApplicableChildNodeDef(NameConstants.JCR_SYSTEM, NameConstants.REP_SYSTEM, ntReg).getId();
        } catch (NoSuchNodeTypeException nsnte) {
            String msg = "internal error: failed to create root node";
            log.error(msg, nsnte);
            throw new ItemStateException(msg, nsnte);
        } catch (ConstraintViolationException cve) {
            String msg = "internal error: failed to create root node";
            log.error(msg, cve);
            throw new ItemStateException(msg, cve);
        }
        rootState.setDefinitionId(nodeDefId);
        jcrSystemState.setDefinitionId(jcrSystemDefId);

         // create jcr:primaryType property on root node state
        rootState.addPropertyName(propDef.getName());
        rootState.addPropertyName(NameConstants.JCR_PRIMARYTYPE);
 
        PropertyState prop = createInstance(propDef.getName(), rootNodeId);
        PropertyState prop = createInstance(NameConstants.JCR_PRIMARYTYPE, rootNodeId);
         prop.setValues(new InternalValue[]{InternalValue.create(NameConstants.REP_ROOT)});
        prop.setType(propDef.getRequiredType());
        prop.setMultiValued(propDef.isMultiple());
        prop.setDefinitionId(propDef.getId());
        prop.setType(PropertyType.NAME);
        prop.setMultiValued(false);
 
         // create jcr:primaryType property on jcr:system node state
        jcrSystemState.addPropertyName(propDef.getName());
        jcrSystemState.addPropertyName(NameConstants.JCR_PRIMARYTYPE);
 
        PropertyState primaryTypeProp = createInstance(propDef.getName(), jcrSystemState.getNodeId());
        PropertyState primaryTypeProp = createInstance(NameConstants.JCR_PRIMARYTYPE, jcrSystemState.getNodeId());
         primaryTypeProp.setValues(new InternalValue[]{InternalValue.create(NameConstants.REP_SYSTEM)});
        primaryTypeProp.setType(propDef.getRequiredType());
        primaryTypeProp.setMultiValued(propDef.isMultiple());
        primaryTypeProp.setDefinitionId(propDef.getId());
        primaryTypeProp.setType(PropertyType.NAME);
        primaryTypeProp.setMultiValued(false);
 
         // add child node entry for jcr:system node
         rootState.addChildNodeEntry(NameConstants.JCR_SYSTEM, RepositoryImpl.SYSTEM_ROOT_NODE_ID);
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/value/InternalValue.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/value/InternalValue.java
index 9f3b59c33..93ccf607f 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/value/InternalValue.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/value/InternalValue.java
@@ -242,6 +242,18 @@ public class InternalValue extends AbstractQValue {
         }
     }
 
    public static InternalValue[] create(QValue[] values)
            throws RepositoryException {
        if (values == null) {
            return null;
        }
        InternalValue[] tmp = new InternalValue[values.length];
        for (int i = 0; i < values.length; i++) {
            tmp[i] = InternalValue.create(values[i]);
        }
        return tmp;
    }

     static InternalValue getInternalValue(DataIdentifier identifier, DataStore store) throws DataStoreException {
         // access the record to ensure it is not garbage collected
         if (store.getRecordIfStored(identifier) != null) {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionManagerImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionManagerImpl.java
index cbbc1cf3d..76317cf81 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionManagerImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/InternalVersionManagerImpl.java
@@ -190,12 +190,8 @@ public class InternalVersionManagerImpl extends InternalVersionManagerBase
             if (false && !pMgr.exists(systemId)) {
                 NodeState root = pMgr.createNew(systemId);
                 root.setParentId(RepositoryImpl.ROOT_NODE_ID);
                root.setDefinitionId(ntReg.getEffectiveNodeType(NameConstants.REP_ROOT).getApplicableChildNodeDef(
                        NameConstants.JCR_SYSTEM, NameConstants.REP_SYSTEM, ntReg).getId());
                 root.setNodeTypeName(NameConstants.REP_SYSTEM);
                 PropertyState pt = pMgr.createNew(new PropertyId(systemId, NameConstants.JCR_PRIMARYTYPE));
                pt.setDefinitionId(ntReg.getEffectiveNodeType(NameConstants.REP_SYSTEM).getApplicablePropertyDef(
                        NameConstants.JCR_PRIMARYTYPE, PropertyType.NAME, false).getId());
                 pt.setMultiValued(false);
                 pt.setType(PropertyType.NAME);
                 pt.setValues(new InternalValue[]{InternalValue.create(NameConstants.REP_SYSTEM)});
@@ -215,12 +211,8 @@ public class InternalVersionManagerImpl extends InternalVersionManagerBase
             if (!pMgr.exists(historiesId)) {
                 NodeState root = pMgr.createNew(historiesId);
                 root.setParentId(systemId);
                root.setDefinitionId(ntReg.getEffectiveNodeType(NameConstants.REP_SYSTEM).getApplicableChildNodeDef(
                        NameConstants.JCR_VERSIONSTORAGE, NameConstants.REP_VERSIONSTORAGE, ntReg).getId());
                 root.setNodeTypeName(NameConstants.REP_VERSIONSTORAGE);
                 PropertyState pt = pMgr.createNew(new PropertyId(historiesId, NameConstants.JCR_PRIMARYTYPE));
                pt.setDefinitionId(ntReg.getEffectiveNodeType(NameConstants.REP_VERSIONSTORAGE).getApplicablePropertyDef(
                        NameConstants.JCR_PRIMARYTYPE, PropertyType.NAME, false).getId());
                 pt.setMultiValued(false);
                 pt.setType(PropertyType.NAME);
                 pt.setValues(new InternalValue[]{InternalValue.create(NameConstants.REP_VERSIONSTORAGE)});
@@ -235,12 +227,8 @@ public class InternalVersionManagerImpl extends InternalVersionManagerBase
             if (!pMgr.exists(activitiesId)) {
                 NodeState root = pMgr.createNew(activitiesId);
                 root.setParentId(systemId);
                root.setDefinitionId(ntReg.getEffectiveNodeType(NameConstants.REP_SYSTEM).getApplicableChildNodeDef(
                        NameConstants.JCR_ACTIVITIES, NameConstants.REP_ACTIVITIES, ntReg).getId());
                 root.setNodeTypeName(NameConstants.REP_ACTIVITIES);
                 PropertyState pt = pMgr.createNew(new PropertyId(activitiesId, NameConstants.JCR_PRIMARYTYPE));
                pt.setDefinitionId(ntReg.getEffectiveNodeType(NameConstants.REP_ACTIVITIES).getApplicablePropertyDef(
                        NameConstants.JCR_PRIMARYTYPE, PropertyType.NAME, false).getId());
                 pt.setMultiValued(false);
                 pt.setType(PropertyType.NAME);
                 pt.setValues(new InternalValue[]{InternalValue.create(NameConstants.REP_ACTIVITIES)});
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/NodeStateEx.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/NodeStateEx.java
index ac6701216..cac4727f1 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/NodeStateEx.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/NodeStateEx.java
@@ -30,10 +30,8 @@ import org.apache.jackrabbit.core.PropertyImpl;
 import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.id.PropertyId;
 import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeDef;
 import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
 import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
 import org.apache.jackrabbit.core.state.ChildNodeEntry;
 import org.apache.jackrabbit.core.state.ItemState;
 import org.apache.jackrabbit.core.state.ItemStateException;
@@ -42,6 +40,8 @@ import org.apache.jackrabbit.core.state.PropertyState;
 import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
 import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
 
 /**
@@ -69,6 +69,11 @@ public class NodeStateEx {
      */
     private Name name;
 
    /**
     * the cached node definition
     */
    private QNodeDefinition def;

     /**
      * Creates a new persistent node
      *
@@ -280,13 +285,9 @@ public class NodeStateEx {
                 throw new RepositoryException("Unable to create property: " + e.toString());
             }
         } else {

            PropDef pd = getEffectiveNodeType().getApplicablePropertyDef(name, type, multiple);

             PropertyState propState = stateMgr.createNew(name, nodeState.getNodeId());
             propState.setType(type);
             propState.setMultiValued(multiple);
            propState.setDefinitionId(pd.getId());
             propState.setValues(values);
 
             // need to store node state
@@ -560,10 +561,6 @@ public class NodeStateEx {
         }
         NodeState state = stateMgr.createNew(id, nodeTypeName, parentId);
 
        NodeDef cnd =
                getEffectiveNodeType().getApplicableChildNodeDef(name, nodeTypeName, ntReg);
        state.setDefinitionId(cnd.getId());

         // create Node instance wrapping new node state
         NodeStateEx node = new NodeStateEx(stateMgr, ntReg, state, name);
         node.setPropertyValue(NameConstants.JCR_PRIMARYTYPE, InternalValue.create(nodeTypeName));
@@ -589,10 +586,11 @@ public class NodeStateEx {
         if (name == null) {
             name = src.getName();
         }
        EffectiveNodeType ent = getEffectiveNodeType();
         // (4) check for name collisions
        NodeDef def;
        QNodeDefinition def;
         try {
            def = getEffectiveNodeType().getApplicableChildNodeDef(name, nodeState.getNodeTypeName(), ntReg);
            def = ent.getApplicableChildNodeDef(name, nodeState.getNodeTypeName(), ntReg);
         } catch (RepositoryException re) {
             String msg = "no definition found in parent node's node type for new node";
             throw new ConstraintViolationException(msg, re);
@@ -611,7 +609,9 @@ public class NodeStateEx {
             } catch (ItemStateException e) {
                 throw new RepositoryException(e);
             }
            if (!ntReg.getNodeDef(existingChild.getDefinitionId()).allowsSameNameSiblings()) {
            QNodeDefinition existingChildDef = ent.getApplicableChildNodeDef(
                    cne.getName(), existingChild.getNodeTypeName(), ntReg);
            if (!existingChildDef.allowsSameNameSiblings()) {
                 throw new ItemExistsException(existingChild.toString());
             }
         } else {
@@ -646,8 +646,7 @@ public class NodeStateEx {
             }
             NodeState srcState = src.getState();
             srcState.setParentId(getNodeId());
            srcState.setDefinitionId(def.getId());
            

             if (srcState.getStatus() == ItemState.STATUS_EXISTING) {
                 srcState.setStatus(ItemState.STATUS_EXISTING_MODIFIED);
             }
@@ -823,20 +822,29 @@ public class NodeStateEx {
     }
 
     /**
     * Returns the NodeDef for this state
     * Returns the QNodeDefinition for this state
      * @return the node def
     * @throws RepositoryException if an error occurs
      */
    public NodeDef getDefinition() {
        return ntReg.getNodeDef(nodeState.getDefinitionId());
    public QNodeDefinition getDefinition() throws RepositoryException {
        if (def == null) {
            EffectiveNodeType ent = getParent().getEffectiveNodeType();
            def = ent.getApplicableChildNodeDef(getName(),
                    nodeState.getNodeTypeName(), ntReg);
        }
        return def;
     }
 
     /**
      * Returns the property definition for the property state
      * @param prop the property state
      * @return the prop def
     * @throws RepositoryException if an error occurs
      */
    public PropDef getDefinition(PropertyState prop) {
        return ntReg.getPropDef(prop.getDefinitionId());
    public QPropertyDefinition getDefinition(PropertyState prop)
            throws RepositoryException {
        return getEffectiveNodeType().getApplicablePropertyDef(
                prop.getName(), prop.getType(), prop.isMultiValued());
     }
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/VersionManagerImplRestore.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/VersionManagerImplRestore.java
index 98f00a69c..2a775acf2 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/VersionManagerImplRestore.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/version/VersionManagerImplRestore.java
@@ -34,7 +34,6 @@ import org.apache.jackrabbit.core.HierarchyManager;
 import org.apache.jackrabbit.core.ItemValidator;
 import org.apache.jackrabbit.core.SessionImpl;
 import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.PropDef;
 import org.apache.jackrabbit.core.security.authorization.Permission;
 import org.apache.jackrabbit.core.state.ChildNodeEntry;
 import org.apache.jackrabbit.core.state.ItemStateException;
@@ -43,6 +42,7 @@ import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
 import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QPropertyDefinition;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -428,7 +428,7 @@ abstract public class VersionManagerImplRestore extends VersionManagerImplBase {
         }
 
         // add 'auto-create' properties that do not exist yet
        for (PropDef def: state.getEffectiveNodeType().getAutoCreatePropDefs()) {
        for (QPropertyDefinition def: state.getEffectiveNodeType().getAutoCreatePropDefs()) {
             if (!state.hasProperty(def.getName())) {
                 InternalValue[] values = computeAutoValues(state, def, true);
                 if (values != null) {
@@ -644,14 +644,16 @@ abstract public class VersionManagerImplRestore extends VersionManagerImplBase {
      * @param def property definition
      * @param useDefaultValues if <code>true</code> the default values are respected
      * @return the values or <code>null</code>
     * @throws RepositoryException if the values cannot be computed.
      */
    private InternalValue[] computeAutoValues(NodeStateEx state, PropDef def,
                                                     boolean useDefaultValues) {
    private InternalValue[] computeAutoValues(NodeStateEx state, QPropertyDefinition def,
                                              boolean useDefaultValues)
            throws RepositoryException {
         // compute system generated values if necessary
         InternalValue[] values = session.getNodeTypeInstanceHandler().
                 computeSystemGeneratedPropertyValues(state.getState(), def);
         if (values == null && useDefaultValues) {
            values = def.getDefaultValues();
            values = InternalValue.create(def.getDefaultValues());
         }
         // avoid empty value array for single value property
         if (values != null && values.length == 0 && !def.isMultiple()) {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/virtual/AbstractVISProvider.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/virtual/AbstractVISProvider.java
index a8bde7e61..f8ef82d50 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/virtual/AbstractVISProvider.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/virtual/AbstractVISProvider.java
@@ -20,11 +20,8 @@ import org.apache.jackrabbit.core.id.ItemId;
 import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.id.PropertyId;
 import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
 import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
 import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
 import org.apache.jackrabbit.core.state.ItemState;
 import org.apache.jackrabbit.core.state.ItemStateException;
 import org.apache.jackrabbit.core.state.NoSuchItemStateException;
@@ -34,8 +31,9 @@ import org.apache.jackrabbit.core.state.ItemStateReferenceMap;
 import org.apache.jackrabbit.core.state.ItemStateListener;
 import org.apache.jackrabbit.core.state.ChildNodeEntry;
 import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
 import org.apache.jackrabbit.util.WeakIdentityCollection;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -262,12 +260,10 @@ public abstract class AbstractVISProvider implements VirtualItemStateProvider, I
                                                     Name name, int type,
                                                     boolean multiValued)
             throws RepositoryException {
        PropDef def = getApplicablePropertyDef(parent, name, type, multiValued);
         PropertyId id = new PropertyId(parent.getNodeId(), name);
         VirtualPropertyState prop = new VirtualPropertyState(id);
         prop.setType(type);
         prop.setMultiValued(multiValued);
        prop.setDefinitionId(def.getId());
         return prop;
     }
 
@@ -278,25 +274,12 @@ public abstract class AbstractVISProvider implements VirtualItemStateProvider, I
                                             NodeId id, Name nodeTypeName)
             throws RepositoryException {
 
        NodeDefId def;
        try {
            def = getApplicableChildNodeDef(parent, name, nodeTypeName).getId();
        } catch (RepositoryException re) {
            // hack, use nt:unstructured as parent
            NodeTypeRegistry ntReg = getNodeTypeRegistry();
            EffectiveNodeType ent = ntReg.getEffectiveNodeType(NameConstants.NT_UNSTRUCTURED);
            NodeDef cnd = ent.getApplicableChildNodeDef(name, nodeTypeName, ntReg);
            ntReg.getNodeDef(cnd.getId());
            def = cnd.getId();
        }

         // create a new node state
         VirtualNodeState state;
         if (id == null) {
             id = new NodeId();
         }
         state = new VirtualNodeState(this, parent.getNodeId(), id, nodeTypeName, new Name[0]);
        state.setDefinitionId(def);
 
         cache(state);
         return state;
@@ -385,7 +368,7 @@ public abstract class AbstractVISProvider implements VirtualItemStateProvider, I
      * @return
      * @throws RepositoryException
      */
    protected PropDef getApplicablePropertyDef(NodeState parent, Name propertyName,
    protected QPropertyDefinition getApplicablePropertyDef(NodeState parent, Name propertyName,
                                                int type, boolean multiValued)
             throws RepositoryException {
         return getEffectiveNodeType(parent).getApplicablePropertyDef(propertyName, type, multiValued);
@@ -400,7 +383,7 @@ public abstract class AbstractVISProvider implements VirtualItemStateProvider, I
      * @return
      * @throws RepositoryException
      */
    protected NodeDef getApplicableChildNodeDef(NodeState parent, Name nodeName, Name nodeTypeName)
    protected QNodeDefinition getApplicableChildNodeDef(NodeState parent, Name nodeName, Name nodeTypeName)
             throws RepositoryException {
         return getEffectiveNodeType(parent).getApplicableChildNodeDef(
                 nodeName, nodeTypeName, getNodeTypeRegistry());
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/DefaultProtectedPropertyImporter.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/DefaultProtectedPropertyImporter.java
index 2574efcd6..c39bbe479 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/DefaultProtectedPropertyImporter.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/DefaultProtectedPropertyImporter.java
@@ -20,9 +20,9 @@ import javax.jcr.RepositoryException;
 
 import org.apache.jackrabbit.core.NodeImpl;
 import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.nodetype.PropDef;
 import org.apache.jackrabbit.api.JackrabbitSession;
 import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.QPropertyDefinition;
 
 /**
  * Default implementation that isn't able to handle any protected properties.
@@ -46,18 +46,18 @@ public class DefaultProtectedPropertyImporter implements ProtectedPropertyImport
     /**
      * Always returns <code>false</code>.
      *
     * @see ProtectedPropertyImporter#handlePropInfo(org.apache.jackrabbit.core.NodeImpl, PropInfo, org.apache.jackrabbit.core.nodetype.PropDef)
     * @see ProtectedPropertyImporter#handlePropInfo(org.apache.jackrabbit.core.NodeImpl, PropInfo, QPropertyDefinition)
      */
    public boolean handlePropInfo(NodeImpl parent, PropInfo protectedPropInfo, PropDef def) {
    public boolean handlePropInfo(NodeImpl parent, PropInfo protectedPropInfo, QPropertyDefinition def) {
         return false;
     }
 
     /**
      * Always returns <code>false</code>.
      *
     * @see ProtectedPropertyImporter#handlePropInfo(org.apache.jackrabbit.core.state.NodeState, PropInfo, PropDef)
     * @see ProtectedPropertyImporter#handlePropInfo(org.apache.jackrabbit.core.state.NodeState, PropInfo, QPropertyDefinition)
      */
    public boolean handlePropInfo(NodeState parent, PropInfo protectedPropInfo, PropDef def) throws RepositoryException {
    public boolean handlePropInfo(NodeState parent, PropInfo protectedPropInfo, QPropertyDefinition def) throws RepositoryException {
         return false;
     }
 }
\ No newline at end of file
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/PropInfo.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/PropInfo.java
index 197e7edb9..07cd9f34c 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/PropInfo.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/PropInfo.java
@@ -23,12 +23,10 @@ import javax.jcr.nodetype.ConstraintViolationException;
 
 import org.apache.jackrabbit.core.NodeImpl;
 import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.PropDef;
 import org.apache.jackrabbit.core.state.NodeState;
 import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
 import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
 /**
  * Information about a property being imported. This class is used
@@ -79,7 +77,7 @@ public class PropInfo {
         }
     }
 
    public int getTargetType(PropDef def) {
    public int getTargetType(QPropertyDefinition def) {
         int target = def.getRequiredType();
         if (target != PropertyType.UNDEFINED) {
             return target;
@@ -90,7 +88,7 @@ public class PropInfo {
         }
     }
 
    public PropDef getApplicablePropertyDef(EffectiveNodeType ent)
    public QPropertyDefinition getApplicablePropertyDef(EffectiveNodeType ent)
             throws ConstraintViolationException {
         if (values.length == 1) {
             // could be single- or multi-valued (n == 1)
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/ProtectedPropertyImporter.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/ProtectedPropertyImporter.java
index d8cf2fc86..bb15e6969 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/ProtectedPropertyImporter.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/ProtectedPropertyImporter.java
@@ -19,12 +19,12 @@ package org.apache.jackrabbit.core.xml;
 import javax.jcr.RepositoryException;
 
 import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.nodetype.PropDef;
 import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.QPropertyDefinition;
 
 /**
  * <code>ProtectedPropertyImporter</code> is in charge of importing single
 * properties whith a protected <code>PropDef</code>.
 * properties whith a protected <code>QPropertyDefinition</code>.
  *
  * @see ProtectedNodeImporter for an abstract class used to import protected
  * nodes and the subtree below them.
@@ -42,7 +42,7 @@ public interface ProtectedPropertyImporter {
      * <code>false</code> otherwise.
      * @throws RepositoryException If an error occurs.
      */
    boolean handlePropInfo(NodeImpl parent, PropInfo protectedPropInfo, PropDef def)
    boolean handlePropInfo(NodeImpl parent, PropInfo protectedPropInfo, QPropertyDefinition def)
             throws RepositoryException;
 
     /**
@@ -56,7 +56,7 @@ public interface ProtectedPropertyImporter {
      * <code>false</code> otherwise.
      * @throws RepositoryException If an error occurs.
      */
    boolean handlePropInfo(NodeState parent, PropInfo protectedPropInfo, PropDef def)
    boolean handlePropInfo(NodeState parent, PropInfo protectedPropInfo, QPropertyDefinition def)
             throws RepositoryException;
 
 
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/SessionImporter.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/SessionImporter.java
index ec069508c..f75da8963 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/SessionImporter.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/SessionImporter.java
@@ -35,10 +35,10 @@ import javax.jcr.nodetype.NodeDefinition;
 import org.apache.jackrabbit.core.NodeImpl;
 import org.apache.jackrabbit.core.SessionImpl;
 import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.PropDef;
 import org.apache.jackrabbit.core.security.authorization.Permission;
 import org.apache.jackrabbit.core.util.ReferenceChangeTracker;
 import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
@@ -153,7 +153,7 @@ public class SessionImporter implements Importer {
     }
 
 
    protected void createProperty(NodeImpl node, PropInfo pInfo, PropDef def) throws RepositoryException {
    protected void createProperty(NodeImpl node, PropInfo pInfo, QPropertyDefinition def) throws RepositoryException {
         // convert serialized values to Value objects
         Value[] va = pInfo.getValues(pInfo.getTargetType(def), session);
 
@@ -369,7 +369,7 @@ public class SessionImporter implements Importer {
 
         for (PropInfo pi : propInfos) {
             // find applicable definition
            PropDef def = pi.getApplicablePropertyDef(node.getEffectiveNodeType());
            QPropertyDefinition def = pi.getApplicablePropertyDef(node.getEffectiveNodeType());
             if (def.isProtected()) {
                 // skip protected property
                 log.debug("Skipping protected property " + pi.getName());
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/WorkspaceImporter.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/WorkspaceImporter.java
index 9b64d0db6..428824a8a 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/WorkspaceImporter.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/xml/WorkspaceImporter.java
@@ -38,9 +38,7 @@ import org.apache.jackrabbit.core.WorkspaceImpl;
 import org.apache.jackrabbit.core.id.NodeId;
 import org.apache.jackrabbit.core.id.PropertyId;
 import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeDef;
 import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
 import org.apache.jackrabbit.core.state.ChildNodeEntry;
 import org.apache.jackrabbit.core.state.NodeState;
 import org.apache.jackrabbit.core.state.PropertyState;
@@ -50,6 +48,8 @@ import org.apache.jackrabbit.core.version.InternalVersionManager;
 import org.apache.jackrabbit.core.version.VersionHistoryInfo;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
 import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
 import org.slf4j.Logger;
@@ -348,7 +348,7 @@ public class WorkspaceImporter implements Importer {
 
     protected void processProperty(NodeState node, PropInfo pInfo) throws RepositoryException {
         PropertyState prop;
        PropDef def;
        QPropertyDefinition def;
 
         Name name = pInfo.getName();
         int type = pInfo.getType();
@@ -357,7 +357,7 @@ public class WorkspaceImporter implements Importer {
             // a property with that name already exists...
             PropertyId idExisting = new PropertyId(node.getNodeId(), name);
             prop = (PropertyState) itemOps.getItemState(idExisting);
            def = ntReg.getPropDef(prop.getDefinitionId());
            def = itemOps.findApplicablePropertyDefinition(prop.getName(), prop.getType(), prop.isMultiValued(), node);
             if (def.isProtected()) {
                 // skip protected property
                 log.debug("skipping protected property "
@@ -429,7 +429,7 @@ public class WorkspaceImporter implements Importer {
             InternalValue value)
             throws RepositoryException {
         if (!node.hasPropertyName(name)) {
            PropDef def = itemOps.findApplicablePropertyDefinition(
            QPropertyDefinition def = itemOps.findApplicablePropertyDefinition(
                     name, type, multiple, node);
             PropertyState prop = itemOps.createPropertyState(
                     node, name, type, def);
@@ -492,7 +492,8 @@ public class WorkspaceImporter implements Importer {
                         parent.getChildNodeEntry(nodeName, 1);
                 NodeId idExisting = entry.getId();
                 NodeState existing = (NodeState) itemOps.getItemState(idExisting);
                NodeDef def = ntReg.getNodeDef(existing.getDefinitionId());
                QNodeDefinition def = itemOps.findApplicableNodeDefinition(
                        nodeName, existing.getNodeTypeName(), parent);
 
                 if (!def.allowsSameNameSiblings()) {
                     // existing doesn't allow same-name siblings,
@@ -528,7 +529,7 @@ public class WorkspaceImporter implements Importer {
                 // there's no node with that name...
                 if (id == null) {
                     // no potential uuid conflict, always create new node
                    NodeDef def = itemOps.findApplicableNodeDefinition(
                    QNodeDefinition def = itemOps.findApplicableNodeDefinition(
                             nodeName, ntName, parent);
                     if (def.isProtected()) {
                         // skip protected node
@@ -561,7 +562,7 @@ public class WorkspaceImporter implements Importer {
                         }
                     } catch (ItemNotFoundException e) {
                         // create new with given uuid
                        NodeDef def = itemOps.findApplicableNodeDefinition(
                        QNodeDefinition def = itemOps.findApplicableNodeDefinition(
                                 nodeName, ntName, parent);
                         if (def.isProtected()) {
                             // skip protected node
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/nodetype/CyclicNodeTypeRegistrationTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/nodetype/CyclicNodeTypeRegistrationTest.java
index 5c5eacae1..2fd2dfbb7 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/nodetype/CyclicNodeTypeRegistrationTest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/nodetype/CyclicNodeTypeRegistrationTest.java
@@ -18,9 +18,13 @@ package org.apache.jackrabbit.core.nodetype;
 
 import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.nodetype.QNodeDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.QPropertyDefinitionBuilder;
 import org.apache.jackrabbit.test.AbstractJCRTest;
 import org.apache.jackrabbit.spi.NameFactory;
 import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
 
 import javax.jcr.PropertyType;
 import javax.jcr.RepositoryException;
@@ -118,18 +122,18 @@ public class CyclicNodeTypeRegistrationTest extends AbstractJCRTest {
         bar.setName(nameFactory.create("", "bar"));
         bar.setSupertypes(new Name[]{NameConstants.NT_BASE});
 
        NodeDefImpl myBarInFoo = new NodeDefImpl();
        QNodeDefinitionBuilder myBarInFoo = new QNodeDefinitionBuilder();
         myBarInFoo.setRequiredPrimaryTypes(new Name[]{bar.getName()});
         myBarInFoo.setName(nameFactory.create("", "myBarInFoo"));
         myBarInFoo.setDeclaringNodeType(foo.getName());
 
        NodeDefImpl myFooInBar = new NodeDefImpl();
        QNodeDefinitionBuilder myFooInBar = new QNodeDefinitionBuilder();
         myFooInBar.setRequiredPrimaryTypes(new Name[]{foo.getName()});
         myFooInBar.setName(nameFactory.create("", "myFooInBar"));
         myFooInBar.setDeclaringNodeType(bar.getName());
 
        foo.setChildNodeDefs(new NodeDefImpl[]{myBarInFoo});
        bar.setChildNodeDefs(new NodeDefImpl[]{myFooInBar});
        foo.setChildNodeDefs(new QNodeDefinition[]{myBarInFoo.build()});
        bar.setChildNodeDefs(new QNodeDefinition[]{myFooInBar.build()});
         ntDefCollection = new LinkedList();
         ntDefCollection.add(foo);
         ntDefCollection.add(bar);
@@ -163,12 +167,12 @@ public class CyclicNodeTypeRegistrationTest extends AbstractJCRTest {
         foo.setSupertypes(new Name[]{NameConstants.NT_BASE});
 
 
        NodeDefImpl myBarInFoo = new NodeDefImpl();
        QNodeDefinitionBuilder myBarInFoo = new QNodeDefinitionBuilder();
         myBarInFoo.setRequiredPrimaryTypes(new Name[]{nameFactory.create("", "I_am_an_invalid_required_primary_type")});
         myBarInFoo.setName(nameFactory.create("", "myNTInFoo"));
         myBarInFoo.setDeclaringNodeType(foo.getName());
 
        foo.setChildNodeDefs(new NodeDefImpl[]{myBarInFoo});
        foo.setChildNodeDefs(new QNodeDefinition[]{myBarInFoo.build()});
         ntDefCollection = new LinkedList();
         ntDefCollection.add(foo);
 
@@ -208,36 +212,38 @@ public class CyclicNodeTypeRegistrationTest extends AbstractJCRTest {
         final NodeTypeDef cmsObject = new NodeTypeDef();
         cmsObject.setName(nameFactory.create("", "CmsObject"));
         cmsObject.setSupertypes(new Name[]{NameConstants.NT_BASE});
        NodeDefImpl parentFolder = new NodeDefImpl();

        QNodeDefinitionBuilder parentFolder = new QNodeDefinitionBuilder();
         parentFolder.setRequiredPrimaryTypes(new Name[]{folder.getName()});
         parentFolder.setName(nameFactory.create("", "parentFolder"));
         parentFolder.setDeclaringNodeType(cmsObject.getName());
        cmsObject.setChildNodeDefs(new NodeDefImpl[]{parentFolder});
        cmsObject.setChildNodeDefs(new QNodeDefinition[]{parentFolder.build()});
 
 
         final NodeTypeDef document = new NodeTypeDef();
         document.setName(nameFactory.create("", "Document"));
         document.setSupertypes(new Name[]{cmsObject.getName()});
        PropDefImpl sizeProp = new PropDefImpl();
        QPropertyDefinitionBuilder sizeProp = new QPropertyDefinitionBuilder();
         sizeProp.setName(nameFactory.create("", "size"));
         sizeProp.setRequiredType(PropertyType.LONG);
         sizeProp.setDeclaringNodeType(document.getName());
        document.setPropertyDefs(new PropDef[]{sizeProp});
        document.setPropertyDefs(new QPropertyDefinition[]{sizeProp.build()});
 
 
         folder.setSupertypes(new Name[]{cmsObject.getName()});
 
        NodeDefImpl folders = new NodeDefImpl();
        QNodeDefinitionBuilder folders = new QNodeDefinitionBuilder();
         folders.setRequiredPrimaryTypes(new Name[]{folder.getName()});
         folders.setName(nameFactory.create("", "folders"));
         folders.setDeclaringNodeType(folder.getName());
 
        NodeDefImpl documents = new NodeDefImpl();
        QNodeDefinitionBuilder documents = new QNodeDefinitionBuilder();
         documents.setRequiredPrimaryTypes(new Name[]{document.getName()});
         documents.setName(nameFactory.create("", "documents"));
         documents.setDeclaringNodeType(folder.getName());
 
        folder.setChildNodeDefs(new NodeDefImpl[]{folders, documents});
        folder.setChildNodeDefs(new QNodeDefinition[]{
                folders.build(), documents.build()});
         ntDefCollection = new LinkedList();
         ntDefCollection.add(folder);
         ntDefCollection.add(document);
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/nodetype/xml/TestAll.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/nodetype/xml/TestAll.java
index 226743030..7e4f511e2 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/nodetype/xml/TestAll.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/nodetype/xml/TestAll.java
@@ -35,16 +35,17 @@ import junit.framework.AssertionFailedError;
 import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
 import org.apache.jackrabbit.commons.cnd.CndImporter;
 import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeDef;
 import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.value.InternalValue;
 import org.apache.jackrabbit.core.value.InternalValueFactory;
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
 import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
 import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
 import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
 import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
 import org.apache.jackrabbit.spi.commons.value.ValueFactoryQImpl;
 import org.apache.jackrabbit.test.AbstractJCRTest;
@@ -130,16 +131,16 @@ public class TestAll extends AbstractJCRTest {
      * @param propertyName property name, or <code>null</code>
      * @return property definition
      */
    private PropDef getPropDef(String typeName, String propertyName) {
    private QPropertyDefinition getPropDef(String typeName, String propertyName) {
         Name name;
         if (propertyName != null) {
             name = FACTORY.create(TEST_NAMESPACE, propertyName);
         } else {
            name = PropDef.ANY_NAME;
            name = NameConstants.ANY_NAME;
         }
 
         NodeTypeDef def = getNodeType(typeName);
        PropDef[] defs = def.getPropertyDefs();
        QPropertyDefinition[] defs = def.getPropertyDefs();
         for (int i = 0; i < defs.length; i++) {
             if (name.equals(defs[i].getName())) {
                 return defs[i];
@@ -157,9 +158,9 @@ public class TestAll extends AbstractJCRTest {
      * @param index default value index
      * @return default value
      */
    private String getDefaultValue(PropDef def, int index) {
    private String getDefaultValue(QPropertyDefinition def, int index) {
         try {
            InternalValue[] values = def.getDefaultValues();
            QValue[] values = def.getDefaultValues();
             NamespaceResolver nsResolver = new AdditionalNamespaceResolver(registry);
             NamePathResolver resolver = new DefaultNamePathResolver(nsResolver);
             ValueFactoryQImpl factory = new ValueFactoryQImpl(InternalValueFactory.getInstance(), resolver);
@@ -178,11 +179,11 @@ public class TestAll extends AbstractJCRTest {
      * @param nodeName child node name
      * @return child node definition
      */
    private NodeDef getChildNode(String typeName, String nodeName) {
    private QNodeDefinition getChildNode(String typeName, String nodeName) {
         Name name = FACTORY.create(TEST_NAMESPACE, nodeName);
 
         NodeTypeDef def = getNodeType(typeName);
        NodeDef[] defs = def.getChildNodeDefs();
        QNodeDefinition[] defs = def.getChildNodeDefs();
         for (int i = 0; i < defs.length; i++) {
             if (name.equals(defs[i].getName())) {
                 return defs[i];
@@ -240,7 +241,7 @@ public class TestAll extends AbstractJCRTest {
                 def.getPrimaryItemName());
         assertEquals("itemNodeType propertyDefs",
                 10, def.getPropertyDefs().length);
        PropDef pdef = getPropDef("itemNodeType", null);
        QPropertyDefinition pdef = getPropDef("itemNodeType", null);
         assertTrue("itemNodeType wildcard property", pdef.definesResidual());
     }
 
@@ -319,7 +320,7 @@ public class TestAll extends AbstractJCRTest {
 
     /** Test for the empty item definition. */
     public void testEmptyItem() {
        PropDef def = getPropDef("itemNodeType", "emptyItem");
        QPropertyDefinition def = getPropDef("itemNodeType", "emptyItem");
         assertEquals("emptyItem autoCreate",
                 false, def.isAutoCreated());
         assertEquals("emptyItem mandatory",
@@ -332,56 +333,56 @@ public class TestAll extends AbstractJCRTest {
 
     /** Test for the <code>autoCreated</code> item definition attribute. */
     public void testAutoCreateItem() {
        PropDef def = getPropDef("itemNodeType", "autoCreatedItem");
        QPropertyDefinition def = getPropDef("itemNodeType", "autoCreatedItem");
         assertEquals("autoCreatedItem autoCreated",
                 true, def.isAutoCreated());
     }
 
     /** Test for the <code>mandatory</code> item definition attribute. */
     public void testMandatoryItem() {
        PropDef def = getPropDef("itemNodeType", "mandatoryItem");
        QPropertyDefinition def = getPropDef("itemNodeType", "mandatoryItem");
         assertEquals("mandatoryItem mandatory",
                 true, def.isMandatory());
     }
 
     /** Test for the <code>copy</code> parent version action. */
     public void testCopyItem() {
        PropDef def = getPropDef("itemNodeType", "copyItem");
        QPropertyDefinition def = getPropDef("itemNodeType", "copyItem");
         assertEquals("copyItem onParentVersion",
                 OnParentVersionAction.COPY, def.getOnParentVersion());
     }
 
     /** Test for the <code>version</code> parent version action. */
     public void testVersionItem() {
        PropDef def = getPropDef("itemNodeType", "versionItem");
        QPropertyDefinition def = getPropDef("itemNodeType", "versionItem");
         assertEquals("versionItem onParentVersion",
                 OnParentVersionAction.VERSION, def.getOnParentVersion());
     }
 
     /** Test for the <code>initialize</code> parent version action. */
     public void testInitializeItem() {
        PropDef def = getPropDef("itemNodeType", "initializeItem");
        QPropertyDefinition def = getPropDef("itemNodeType", "initializeItem");
         assertEquals("initializeItem onParentVersion",
                 OnParentVersionAction.INITIALIZE, def.getOnParentVersion());
     }
 
     /** Test for the <code>compute</code> parent version action. */
     public void testComputeItem() {
        PropDef def = getPropDef("itemNodeType", "computeItem");
        QPropertyDefinition def = getPropDef("itemNodeType", "computeItem");
         assertEquals("computeItem onParentVersion",
                 OnParentVersionAction.COMPUTE, def.getOnParentVersion());
     }
 
     /** Test for the <code>abort</code> parent version action. */
     public void testAbortItem() {
        PropDef def = getPropDef("itemNodeType", "abortItem");
        QPropertyDefinition def = getPropDef("itemNodeType", "abortItem");
         assertEquals("abortItem onParentVersion",
                 OnParentVersionAction.ABORT, def.getOnParentVersion());
     }
 
     /** Test for the <code>protected</code> item definition attribute. */
     public void testProtectedItem() {
        PropDef def = getPropDef("itemNodeType", "protectedItem");
        QPropertyDefinition def = getPropDef("itemNodeType", "protectedItem");
         assertEquals("protectedItem protected",
                 true, def.isProtected());
     }
@@ -395,33 +396,33 @@ public class TestAll extends AbstractJCRTest {
 
     /** Test for the empty property definition. */
     public void testEmptyProperty() {
        PropDef def = getPropDef("propertyNodeType", "emptyProperty");
        QPropertyDefinition def = getPropDef("propertyNodeType", "emptyProperty");
         assertEquals("emptyProperty requiredType",
                 PropertyType.UNDEFINED, def.getRequiredType());
         assertEquals("emptyProperty multiple",
                 false, def.isMultiple());
        assertEquals("emptyProperty defaultValues",
                0, def.getDefaultValues().length);
        assertNull("emptyProperty defaultValues",
                def.getDefaultValues());
         assertEquals("emptyProperty valueConstraints",
                 0, def.getValueConstraints().length);
     }
 
     /** Test for the <code>binary</code> property definition type. */
     public void testBinaryProperty() {
        PropDef def = getPropDef("propertyNodeType", "binaryProperty");
        QPropertyDefinition def = getPropDef("propertyNodeType", "binaryProperty");
         assertEquals("binaryProperty requiredType",
                 PropertyType.BINARY, def.getRequiredType());
         assertEquals("binaryProperty valueConstraints",
                 1, def.getValueConstraints().length);
         assertEquals("binaryProperty valueConstraints[0]",
                 "[0,)", (def.getValueConstraints())[0].getString());
        assertEquals("binaryProperty defaultValues",
                0, def.getDefaultValues().length);
        assertNull("binaryProperty defaultValues",
                def.getDefaultValues());
     }
 
     /** Test for the <code>boolean</code> property definition type. */
     public void testBooleanProperty() {
        PropDef def = getPropDef("propertyNodeType", "booleanProperty");
        QPropertyDefinition def = getPropDef("propertyNodeType", "booleanProperty");
         assertEquals("booleanProperty requiredType",
                 PropertyType.BOOLEAN, def.getRequiredType());
         assertEquals("booleanProperty valueConstraints",
@@ -438,7 +439,7 @@ public class TestAll extends AbstractJCRTest {
 
     /** Test for the <code>date</code> property definition type. */
     public void testDateProperty() {
        PropDef def = getPropDef("propertyNodeType", "dateProperty");
        QPropertyDefinition def = getPropDef("propertyNodeType", "dateProperty");
         assertEquals("dateProperty requiredType",
                 PropertyType.DATE, def.getRequiredType());
         assertEquals("dateProperty valueConstraints",
@@ -454,7 +455,7 @@ public class TestAll extends AbstractJCRTest {
 
     /** Test for the <code>double</code> property definition type. */
     public void testDoubleProperty() {
        PropDef def = getPropDef("propertyNodeType", "doubleProperty");
        QPropertyDefinition def = getPropDef("propertyNodeType", "doubleProperty");
         assertEquals("doubleProperty requiredType",
                 PropertyType.DOUBLE, def.getRequiredType());
         assertEquals("doubleProperty valueConstraints",
@@ -473,7 +474,7 @@ public class TestAll extends AbstractJCRTest {
 
     /** Test for the <code>long</code> property definition type. */
     public void testLongProperty() {
        PropDef def = getPropDef("propertyNodeType", "longProperty");
        QPropertyDefinition def = getPropDef("propertyNodeType", "longProperty");
         assertEquals("longProperty requiredType",
                 PropertyType.LONG, def.getRequiredType());
         assertEquals("longProperty valueConstraints",
@@ -492,7 +493,7 @@ public class TestAll extends AbstractJCRTest {
 
     /** Test for the <code>name</code> property definition type. */
     public void testNameProperty() {
        PropDef def = getPropDef("propertyNodeType", "nameProperty");
        QPropertyDefinition def = getPropDef("propertyNodeType", "nameProperty");
         assertEquals("nameProperty requiredType",
                 PropertyType.NAME, def.getRequiredType());
         assertEquals("nameProperty valueConstraints",
@@ -508,7 +509,7 @@ public class TestAll extends AbstractJCRTest {
 
     /** Test for the <code>path</code> property definition type. */
     public void testPathProperty() {
        PropDef def = getPropDef("propertyNodeType", "pathProperty");
        QPropertyDefinition def = getPropDef("propertyNodeType", "pathProperty");
         assertEquals("pathProperty requiredType",
                 PropertyType.PATH, def.getRequiredType());
         assertEquals("pathProperty valueConstraints",
@@ -516,13 +517,13 @@ public class TestAll extends AbstractJCRTest {
         assertEquals("pathProperty valueConstraints[0]",
                 "{}\t{http://www.apache.org/jackrabbit/test}testPath",
                 (def.getValueConstraints())[0].getString());
        assertEquals("pathProperty defaultValues",
                0, def.getDefaultValues().length);
        assertNull("pathProperty defaultValues",
                def.getDefaultValues());
     }
 
     /** Test for the <code>path</code> property definition type. */
     public void testPathProperty1() {
        PropDef def = getPropDef("propertyNodeType", "pathProperty1");
        QPropertyDefinition def = getPropDef("propertyNodeType", "pathProperty1");
         assertEquals("pathProperty requiredType",
                 PropertyType.PATH, def.getRequiredType());
         assertEquals("pathProperty valueConstraints",
@@ -530,13 +531,13 @@ public class TestAll extends AbstractJCRTest {
         assertEquals("pathProperty valueConstraints[0]",
                 "{}\t{http://www.apache.org/jackrabbit/test}testPath\t{}*",
                 (def.getValueConstraints())[0].getString());
        assertEquals("pathProperty defaultValues",
                0, def.getDefaultValues().length);
        assertNull("pathProperty defaultValues",
                def.getDefaultValues());
     }
 
     /** Test for the <code>path</code> property definition type. */
     public void testPathProperty2() {
        PropDef def = getPropDef("propertyNodeType", "pathProperty2");
        QPropertyDefinition def = getPropDef("propertyNodeType", "pathProperty2");
         assertEquals("pathProperty requiredType",
                 PropertyType.PATH, def.getRequiredType());
         assertEquals("pathProperty valueConstraints",
@@ -544,13 +545,13 @@ public class TestAll extends AbstractJCRTest {
         assertEquals("pathProperty valueConstraints[0]",
                 "{http://www.apache.org/jackrabbit/test}testPath\t{}*",
                 (def.getValueConstraints())[0].getString());
        assertEquals("pathProperty defaultValues",
                0, def.getDefaultValues().length);
        assertNull("pathProperty defaultValues",
                def.getDefaultValues());
     }
 
     /** Test for the <code>reference</code> property definition type. */
     public void testReferenceProperty() {
        PropDef def = getPropDef("propertyNodeType", "referenceProperty");
        QPropertyDefinition def = getPropDef("propertyNodeType", "referenceProperty");
         assertEquals("referenceProperty requiredType",
                 PropertyType.REFERENCE, def.getRequiredType());
         assertEquals("referenceProperty valueConstraints",
@@ -558,13 +559,13 @@ public class TestAll extends AbstractJCRTest {
         assertEquals("referenceProperty valueConstraints[0]",
                 "{http://www.jcp.org/jcr/nt/1.0}base",
                 (def.getValueConstraints())[0].getString());
        assertEquals("referenceProperty defaultValues",
                0, def.getDefaultValues().length);
        assertNull("referenceProperty defaultValues",
                def.getDefaultValues());
     }
 
     /** Test for the <code>string</code> property definition type. */
     public void testStringProperty() {
        PropDef def = getPropDef("propertyNodeType", "stringProperty");
        QPropertyDefinition def = getPropDef("propertyNodeType", "stringProperty");
         assertEquals("stringProperty requiredType",
                 PropertyType.STRING, def.getRequiredType());
         assertEquals("stringProperty valueConstraints",
@@ -582,7 +583,7 @@ public class TestAll extends AbstractJCRTest {
 
     /** Test for the <code>multiple</code> property definition attribute. */
     public void testMultipleProperty() {
        PropDef def = getPropDef("propertyNodeType", "multipleProperty");
        QPropertyDefinition def = getPropDef("propertyNodeType", "multipleProperty");
         assertEquals("multipleProperty multiple",
                 true, def.isMultiple());
     }
@@ -596,7 +597,7 @@ public class TestAll extends AbstractJCRTest {
 
     /** Test for the empty child node definition. */
     public void testEmptyNode() {
        NodeDef def = getChildNode("childNodeType", "emptyNode");
        QNodeDefinition def = getChildNode("childNodeType", "emptyNode");
         assertEquals("emptyNode allowsSameNameSiblings",
                 false, def.allowsSameNameSiblings());
         assertEquals("emptyNode defaultPrimaryType",
@@ -605,14 +606,14 @@ public class TestAll extends AbstractJCRTest {
 
     /** Test for the <code>allowsSameNameSiblings</code> child node attribute. */
     public void testSiblingNode() {
        NodeDef def = getChildNode("childNodeType", "siblingNode");
        QNodeDefinition def = getChildNode("childNodeType", "siblingNode");
         assertEquals("siblingNode allowsSameNameSiblings",
                 true, def.allowsSameNameSiblings());
     }
 
     /** Test for the <code>defaultPrimaryType</code> child node attribute. */
     public void testDefaultTypeNode() {
        NodeDef def = getChildNode("childNodeType", "defaultTypeNode");
        QNodeDefinition def = getChildNode("childNodeType", "defaultTypeNode");
         assertEquals("defaultTypeNode defaultPrimaryType",
                 FACTORY.create(Name.NS_NT_URI, "base"),
                 def.getDefaultPrimaryType());
@@ -620,7 +621,7 @@ public class TestAll extends AbstractJCRTest {
 
     /** Test for the <code>requiredPrimaryTypes</code> child node attributes. */
     public void testRequiredTypeNode() {
        NodeDef def = getChildNode("childNodeType", "requiredTypeNode");
        QNodeDefinition def = getChildNode("childNodeType", "requiredTypeNode");
         assertEquals("requiredTypeNode requiredPrimaryTypes",
                 2, def.getRequiredPrimaryTypes().length);
         Name[] types = def.getRequiredPrimaryTypes();
diff --git a/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/QItemDefinitionImpl.java b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/QItemDefinitionImpl.java
index 9826582fb..c0d7458c9 100644
-- a/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/QItemDefinitionImpl.java
++ b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/QItemDefinitionImpl.java
@@ -20,7 +20,6 @@ import org.apache.jackrabbit.spi.QItemDefinition;
 import org.apache.jackrabbit.spi.QNodeDefinition;
 import org.apache.jackrabbit.spi.QPropertyDefinition;
 import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
 
 import java.io.Serializable;
 
@@ -31,12 +30,6 @@ import java.io.Serializable;
  */
 public abstract class QItemDefinitionImpl implements QItemDefinition, Serializable {
 
    /**
     * The special wildcard name used as the name of residual item definitions.
     * TODO don't rely on specific factory impl
     */
    public static final Name ANY_NAME = NameFactoryImpl.getInstance().create("", "*");

     /**
      * The name of the child item.
      */
diff --git a/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/QNodeDefinitionImpl.java b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/QNodeDefinitionImpl.java
index fb2c95a7f..4bb63981d 100644
-- a/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/QNodeDefinitionImpl.java
++ b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/QNodeDefinitionImpl.java
@@ -21,6 +21,8 @@ import org.apache.jackrabbit.spi.Name;
 
 import java.util.Arrays;
 import java.util.TreeSet;
import java.util.Set;
import java.util.HashSet;
 
 /**
  * <code>QNodeDefinitionImpl</code> implements a <code>QNodeDefinition</code>.
@@ -35,8 +37,7 @@ public class QNodeDefinitionImpl extends QItemDefinitionImpl implements QNodeDef
     /**
      * The names of the required primary types.
      */
    private final Name[] requiredPrimaryTypes;

    private final Set<Name> requiredPrimaryTypes = new HashSet<Name>();
     /**
      * The 'allowsSameNameSiblings' flag.
      */
@@ -77,7 +78,7 @@ public class QNodeDefinitionImpl extends QItemDefinitionImpl implements QNodeDef
         super(name, declaringNodeType, isAutoCreated, isMandatory,
                 onParentVersion, isProtected);
         this.defaultPrimaryType = defaultPrimaryType;
        this.requiredPrimaryTypes = requiredPrimaryTypes;
        this.requiredPrimaryTypes.addAll(Arrays.asList(requiredPrimaryTypes));
         this.allowsSameNameSiblings = allowsSameNameSiblings;
     }
 
@@ -93,7 +94,7 @@ public class QNodeDefinitionImpl extends QItemDefinitionImpl implements QNodeDef
      * {@inheritDoc}
      */
     public Name[] getRequiredPrimaryTypes() {
        return requiredPrimaryTypes;
        return requiredPrimaryTypes.toArray(new Name[requiredPrimaryTypes.size()]);
     }
 
     /**
@@ -130,7 +131,8 @@ public class QNodeDefinitionImpl extends QItemDefinitionImpl implements QNodeDef
         if (obj instanceof QNodeDefinition) {
             QNodeDefinition other = (QNodeDefinition) obj;
             return super.equals(obj)
                    && Arrays.equals(requiredPrimaryTypes, other.getRequiredPrimaryTypes())
                    && requiredPrimaryTypes.equals(new HashSet<Name>(
                            Arrays.asList(other.getRequiredPrimaryTypes())))
                     && (defaultPrimaryType == null
                             ? other.getDefaultPrimaryType() == null
                             : defaultPrimaryType.equals(other.getDefaultPrimaryType()))
diff --git a/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/QNodeTypeDefinitionImpl.java b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/QNodeTypeDefinitionImpl.java
index 8f9feaf25..1f337e52e 100644
-- a/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/QNodeTypeDefinitionImpl.java
++ b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/QNodeTypeDefinitionImpl.java
@@ -25,6 +25,7 @@ import org.apache.jackrabbit.spi.QValue;
 import org.apache.jackrabbit.spi.QValueFactory;
 import org.apache.jackrabbit.spi.QValueConstraint;
 import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
 import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
 import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
 import org.apache.jackrabbit.spi.commons.value.ValueFormat;
@@ -40,23 +41,28 @@ import java.util.Collection;
 import java.util.HashSet;
 import java.util.Collections;
 import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Set;
 import java.io.Serializable;
 
 /**
  * <code>QNodeTypeDefinitionImpl</code> implements a serializable SPI node
  * type definition.
  */
public class QNodeTypeDefinitionImpl implements QNodeTypeDefinition, Serializable {
public class QNodeTypeDefinitionImpl implements QNodeTypeDefinition, Serializable, Cloneable {

    private static final long serialVersionUID = -4065300714874671511L;
 
     /**
      * The name of the node definition.
      */
    private final Name name;
    private Name name;
 
     /**
      * The names of the declared super types of this node type definition.
      */
    private final Name[] supertypes;
    private Name[] supertypes;
 
     /**
      * The names of the supported mixins on this node type (or <code>null</code>)
@@ -66,44 +72,51 @@ public class QNodeTypeDefinitionImpl implements QNodeTypeDefinition, Serializabl
     /**
      * Indicates whether this is a mixin node type definition.
      */
    private final boolean isMixin;
    private boolean isMixin;
 
     /**
      * Indicates whether this is an abstract node type definition.
      */
    private final boolean isAbstract;
    private boolean isAbstract;
 
     /**
      * Indicates whether this is a queryable node type definition.
      */
    private final boolean isQueryable;
    private boolean isQueryable;
 
     /**
      * Indicates whether this node type definition has orderable child nodes.
      */
    private final boolean hasOrderableChildNodes;
    private boolean hasOrderableChildNodes;
 
     /**
      * The name of the primary item or <code>null</code> if none is defined.
      */
    private final Name primaryItemName;
    private Name primaryItemName;
 
     /**
     * The list of property definitions.
     * The list of child node definitions.
      */
    private final QPropertyDefinition[] propertyDefs;
    private final Set<QPropertyDefinition> propertyDefs;
 
     /**
     * The list of child node definitions.
     * The list of property definitions.
      */
    private final QNodeDefinition[] childNodeDefs;

    private final Set<QNodeDefinition> childNodeDefs;
     /**
      * Unmodifiable collection of dependent node type <code>Name</code>s.
      * @see #getDependencies()
      */
     private transient Collection<Name> dependencies;
 
    /**
     * Default constructor.
     */
    public QNodeTypeDefinitionImpl() {
        this(null, Name.EMPTY_ARRAY, null, false, false, true, false, null,
                QPropertyDefinition.EMPTY_ARRAY, QNodeDefinition.EMPTY_ARRAY);
    }

     /**
      * Copy constructor.
      *
@@ -143,7 +156,6 @@ public class QNodeTypeDefinitionImpl implements QNodeTypeDefinition, Serializabl
                                    QPropertyDefinition[] declaredPropDefs,
                                    QNodeDefinition[] declaredNodeDefs) {
         this.name = name;
        this.supertypes = supertypes;
         this.supportedMixins = supportedMixins;
         this.isMixin = isMixin;
         this.isAbstract = isAbstract;
@@ -152,6 +164,7 @@ public class QNodeTypeDefinitionImpl implements QNodeTypeDefinition, Serializabl
         this.primaryItemName = primaryItemName;
         this.propertyDefs = getSerializablePropertyDefs(declaredPropDefs);
         this.childNodeDefs = getSerializableNodeDefs(declaredNodeDefs);
        setSupertypes(supertypes); // make sure supertypes are sorted
     }
 
     /**
@@ -194,7 +207,105 @@ public class QNodeTypeDefinitionImpl implements QNodeTypeDefinition, Serializabl
                 createQNodeDefinitions(name, def.getDeclaredChildNodeDefinitions(), resolver));
     }
 
    /**
     * Sets the name of the node type being defined.
     *
     * @param name The name of the node type.
     */
    public void setName(Name name) {
        this.name = name;
    }

    /**
     * Sets the supertypes.
     *
     * @param names the names of the supertypes.
     */
    public void setSupertypes(Name[] names) {
        resetDependencies();
        // Optimize common cases (zero or one supertypes)
        if (names.length == 0) {
            supertypes = Name.EMPTY_ARRAY;
        } else if (names.length == 1) {
            supertypes = new Name[] { names[0] };
        } else {
            // Sort and remove duplicates
            SortedSet<Name> types = new TreeSet<Name>();
            types.addAll(Arrays.asList(names));
            supertypes = types.toArray(new Name[types.size()]);
        }
    }

    /**
     * Sets the mixin flag.
     *
     * @param mixin flag
     */
    public void setMixin(boolean mixin) {
        this.isMixin = mixin;
    }

    /**
     * Sets the orderableChildNodes flag.
     *
     * @param orderableChildNodes flag
     */
    public void setOrderableChildNodes(boolean orderableChildNodes) {
        this.hasOrderableChildNodes = orderableChildNodes;
    }

    /**
     * Sets the 'abstract' flag.
     *
     * @param abstractStatus flag
     */
    public void setAbstract(boolean abstractStatus) {
        this.isAbstract = abstractStatus;
    }

    /**
     * Sets the 'queryable' flag.
     *
     * @param queryable flag
     */
    public void setQueryable(boolean queryable) {
        this.isQueryable = queryable;
    }

    /**
     * Sets the name of the primary item (one of the child items of the node's
     * of this node type)
     *
     * @param primaryItemName The name of the primary item.
     */
    public void setPrimaryItemName(Name primaryItemName) {
        this.primaryItemName = primaryItemName;
    }

    /**
     * Sets the property definitions.
     *
     * @param defs An array of <code>QPropertyDefinition</code> objects.
     */
    public void setPropertyDefs(QPropertyDefinition[] defs) {
        resetDependencies();
        propertyDefs.clear();
        propertyDefs.addAll(Arrays.asList(defs));
    }

    /**
     * Sets the child node definitions.
     *
     * @param defs An array of <code>QNodeDefinition</code> objects
     */
    public void setChildNodeDefs(QNodeDefinition[] defs) {
        resetDependencies();
        childNodeDefs.clear();
        childNodeDefs.addAll(Arrays.asList(defs));
    }

     //------------------------------------------------< QNodeTypeDefinition >---

     /**
      * {@inheritDoc}
      */
@@ -206,9 +317,12 @@ public class QNodeTypeDefinitionImpl implements QNodeTypeDefinition, Serializabl
      * {@inheritDoc}
      */
     public Name[] getSupertypes() {
        Name[] sTypes = new Name[supertypes.length];
        System.arraycopy(supertypes, 0, sTypes, 0, supertypes.length);
        return sTypes;
        if (supertypes.length > 0
                || isMixin() || NameConstants.NT_BASE.equals(getName())) {
            return supertypes;
        } else {
            return new Name[] { NameConstants.NT_BASE };
        }
     }
 
     /**
@@ -250,24 +364,20 @@ public class QNodeTypeDefinitionImpl implements QNodeTypeDefinition, Serializabl
      * {@inheritDoc}
      */
     public QPropertyDefinition[] getPropertyDefs() {
        QPropertyDefinition[] pDefs = new QPropertyDefinition[propertyDefs.length];
        System.arraycopy(propertyDefs, 0, pDefs, 0, propertyDefs.length);
        return pDefs;
        return propertyDefs.toArray(new QPropertyDefinition[propertyDefs.size()]);
     }
 
     /**
      * {@inheritDoc}
      */
     public QNodeDefinition[] getChildNodeDefs() {
        QNodeDefinition[] cnDefs = new QNodeDefinition[childNodeDefs.length];
        System.arraycopy(childNodeDefs, 0, cnDefs, 0, childNodeDefs.length);
        return cnDefs;
        return childNodeDefs.toArray(new QNodeDefinition[childNodeDefs.size()]);
     }
 
     /**
      * {@inheritDoc}
      */
    public Collection getDependencies() {
    public Collection<Name> getDependencies() {
         if (dependencies == null) {
             Collection<Name> deps = new HashSet<Name>();
             // supertypes
@@ -320,46 +430,92 @@ public class QNodeTypeDefinitionImpl implements QNodeTypeDefinition, Serializabl
         }
     }
     
    //-------------------------------------------< java.lang.Object overrides >

    public QNodeTypeDefinitionImpl clone() {
        try {
            // todo: itemdefs should be cloned as well, since mutable
            return (QNodeTypeDefinitionImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            // does not happen, this class is cloneable
            throw new InternalError();
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof QNodeTypeDefinitionImpl) {
            QNodeTypeDefinitionImpl other = (QNodeTypeDefinitionImpl) obj;
            return (name == null ? other.name == null : name.equals(other.name))
                    && (primaryItemName == null ? other.primaryItemName == null : primaryItemName.equals(other.primaryItemName))
                    && Arrays.equals(getSupertypes(), other.getSupertypes())
                    && isMixin == other.isMixin
                    && hasOrderableChildNodes == other.hasOrderableChildNodes
                    && isAbstract == other.isAbstract
                    && isQueryable == other.isQueryable
                    && propertyDefs.equals(other.propertyDefs)
                    && childNodeDefs.equals(other.childNodeDefs);
        }
        return false;
    }

    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }

     //-------------------------------< internal >-------------------------------
 
    private void resetDependencies() {
        dependencies = null;
    }

     /**
     * Returns an array of serializable property definitions for
     * Returns a set of serializable property definitions for
      * <code>propDefs</code>.
      *
      * @param propDefs the SPI property definitions.
     * @return an array of serializable property definitions.
     * @return a set of serializable property definitions.
      */
    private static QPropertyDefinition[] getSerializablePropertyDefs(
    private static Set<QPropertyDefinition> getSerializablePropertyDefs(
             QPropertyDefinition[] propDefs) {
        QPropertyDefinition[] serDefs = new QPropertyDefinition[propDefs.length];
        for (int i = 0; i < propDefs.length; i++) {
            if (propDefs[i] instanceof Serializable) {
                serDefs[i] = propDefs[i];
        Set<QPropertyDefinition> defs = new HashSet<QPropertyDefinition>();
        for (QPropertyDefinition pd : propDefs) {
            if (pd instanceof Serializable) {
                defs.add(pd);
             } else {
                serDefs[i] = new QPropertyDefinitionImpl(propDefs[i]);
                defs.add(pd);
             }
         }
        return serDefs;
        return defs;
     }
 
     /**
     * Returns an array of serializable node definitions for
     * Returns a set of serializable node definitions for
      * <code>nodeDefs</code>.
      *
      * @param nodeDefs the node definitions.
     * @return an array of serializable node definitions.
     * @return a set of serializable node definitions.
      */
    private static QNodeDefinition[] getSerializableNodeDefs(
    private static Set<QNodeDefinition> getSerializableNodeDefs(
             QNodeDefinition[] nodeDefs) {
        QNodeDefinition[] serDefs = new QNodeDefinition[nodeDefs.length];
        for (int i = 0; i < nodeDefs.length; i++) {
            if (nodeDefs[i] instanceof Serializable) {
                serDefs[i] = nodeDefs[i];
        Set<QNodeDefinition> defs = new HashSet<QNodeDefinition>();
        for (QNodeDefinition nd : nodeDefs) {
            if (nd instanceof Serializable) {
                defs.add(nd);
             } else {
                serDefs[i] = new QNodeDefinitionImpl(nodeDefs[i]);
                defs.add(new QNodeDefinitionImpl(nd));
             }
         }
        return serDefs;
        return defs;
     }
 
     private static Name[] getNames(String[] jcrNames, NamePathResolver resolver) throws NamespaceException, IllegalNameException {
@@ -381,8 +537,8 @@ public class QNodeTypeDefinitionImpl implements QNodeTypeDefinition, Serializabl
         QPropertyDefinition[] declaredPropDefs = new QPropertyDefinition[pds.length];
         for (int i = 0; i < pds.length; i++) {
             PropertyDefinition propDef = pds[i];
            Name name = propDef.getName().equals(QItemDefinitionImpl.ANY_NAME.getLocalName())
                    ? QItemDefinitionImpl.ANY_NAME
            Name name = propDef.getName().equals(NameConstants.ANY_NAME.getLocalName())
                    ? NameConstants.ANY_NAME
                     : resolver.getQName(propDef.getName());
             // check if propDef provides declaring node type and if it matches 'this' one.
             if (propDef.getDeclaringNodeType() != null) {
@@ -429,8 +585,8 @@ public class QNodeTypeDefinitionImpl implements QNodeTypeDefinition, Serializabl
         QNodeDefinition[] declaredNodeDefs = new QNodeDefinition[nds.length];
         for (int i = 0; i < nds.length; i++) {
             NodeDefinition nodeDef = nds[i];
            Name name = nodeDef.getName().equals(QItemDefinitionImpl.ANY_NAME.getLocalName())
                    ? QItemDefinitionImpl.ANY_NAME
            Name name = nodeDef.getName().equals(NameConstants.ANY_NAME.getLocalName())
                    ? NameConstants.ANY_NAME
                     : resolver.getQName(nodeDef.getName());
             // check if propDef provides declaring node type and if it matches 'this' one.
             if (nodeDef.getDeclaringNodeType() != null) {
diff --git a/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/NodeDefinitionImpl.java b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/NodeDefinitionImpl.java
index 9c6828c90..add410450 100644
-- a/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/NodeDefinitionImpl.java
++ b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/NodeDefinitionImpl.java
@@ -63,7 +63,17 @@ public class NodeDefinitionImpl extends ItemDefinitionImpl implements NodeDefini
         super(itemDef, ntMgr, resolver);
     }
 
    /**
     * Returns the wrapped node definition.
     *
     * @return the wrapped node definition.
     */
    public QNodeDefinition unwrap() {
        return (QNodeDefinition) itemDef;
    }

     //-------------------------------------------------------< NodeDefinition >

     /**
      * {@inheritDoc}
      */
diff --git a/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/PropertyDefinitionImpl.java b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/PropertyDefinitionImpl.java
index ff343e552..3feeda985 100644
-- a/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/PropertyDefinitionImpl.java
++ b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/PropertyDefinitionImpl.java
@@ -72,7 +72,17 @@ public class PropertyDefinitionImpl extends ItemDefinitionImpl implements Proper
         this.valueFactory = valueFactory;
     }
 
    /**
     * Returns the wrapped property definition.
     *
     * @return the wrapped property definition.
     */
    public QPropertyDefinition unwrap() {
        return (QPropertyDefinition) itemDef;
    }

     //-------------------------------------------------< PropertyDefinition >---

     /**
      * {@inheritDoc}
      */
diff --git a/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/QDefinitionBuilderFactory.java b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/QDefinitionBuilderFactory.java
index 70e251b54..c63bf0656 100644
-- a/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/QDefinitionBuilderFactory.java
++ b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/QDefinitionBuilderFactory.java
@@ -327,10 +327,6 @@ public class QDefinitionBuilderFactory extends DefinitionBuilderFactory<QNodeTyp
 
         @Override
         public void build() {
            if (builder.getRequiredPrimaryTypes() == null) {
                builder.addRequiredPrimaryType(NameConstants.NT_BASE);
            }

             ntd.childNodeDefs.add(builder.build());
         }
     }
diff --git a/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/QItemDefinitionBuilder.java b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/QItemDefinitionBuilder.java
index 02aadcf25..bea5d96ad 100644
-- a/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/QItemDefinitionBuilder.java
++ b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/QItemDefinitionBuilder.java
@@ -17,21 +17,23 @@
 package org.apache.jackrabbit.spi.commons.nodetype;
 
 import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.version.OnParentVersionAction;
 
 import org.apache.jackrabbit.spi.QItemDefinition;
 import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
 
 /**
  * A builder for {@link QItemDefinition}.
  */
 public abstract class QItemDefinitionBuilder {
 
    private Name name;
    private Name declaringType;
    private boolean isAutocreated;
    private int onParentVersion;
    private boolean isProtected;
    private boolean isMandatory;
    private Name name = NameConstants.ANY_NAME;
    private Name declaringType = null;
    private boolean isAutocreated = false;
    private int onParentVersion = OnParentVersionAction.COPY;
    private boolean isProtected = false;
    private boolean isMandatory = false;
 
     /**
      * @param name  the name of the child item definition being build
diff --git a/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/QNodeDefinitionBuilder.java b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/QNodeDefinitionBuilder.java
index 8808a4b6c..a88569914 100644
-- a/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/QNodeDefinitionBuilder.java
++ b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/QNodeDefinitionBuilder.java
@@ -16,13 +16,14 @@
  */
 package org.apache.jackrabbit.spi.commons.nodetype;
 
import java.util.List;
import java.util.ArrayList;
 import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
 
 import org.apache.jackrabbit.spi.Name;
 import org.apache.jackrabbit.spi.QNodeDefinition;
 import org.apache.jackrabbit.spi.commons.QNodeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
 
 /**
  * A builder for a {@link QNodeDefinition}.
@@ -30,7 +31,7 @@ import org.apache.jackrabbit.spi.commons.QNodeDefinitionImpl;
 public class QNodeDefinitionBuilder extends QItemDefinitionBuilder {
 
     private Name defaultPrimaryType;
    private List<Name> requiredPrimaryTypes;
    private Set<Name> requiredPrimaryTypes = new HashSet<Name>();
     private boolean allowsSameNameSiblings;
 
     /**
@@ -55,9 +56,6 @@ public class QNodeDefinitionBuilder extends QItemDefinitionBuilder {
      * @param name the name of a required primary type.
      */
     public void addRequiredPrimaryType(Name name) {
        if (requiredPrimaryTypes == null) {
            requiredPrimaryTypes = new ArrayList<Name>();
        }
         requiredPrimaryTypes.add(name);
     }
 
@@ -66,10 +64,9 @@ public class QNodeDefinitionBuilder extends QItemDefinitionBuilder {
      *              definition being built.
      */
     public void setRequiredPrimaryTypes(Name[] names) {
        if (names == null) {
            requiredPrimaryTypes = null;
        } else {
            requiredPrimaryTypes = new ArrayList<Name>(Arrays.asList(names));
        requiredPrimaryTypes.clear();
        if (names != null) {
            requiredPrimaryTypes.addAll(Arrays.asList(names));
         }
     }
 
@@ -78,8 +75,8 @@ public class QNodeDefinitionBuilder extends QItemDefinitionBuilder {
      *         definition being built.
      */
     public Name[] getRequiredPrimaryTypes() {
        if (requiredPrimaryTypes == null) {
            return null;
        if (requiredPrimaryTypes.isEmpty()) {
            return new Name[]{NameConstants.NT_BASE};
         } else {
             return requiredPrimaryTypes.toArray(new Name[requiredPrimaryTypes.size()]);
         }
@@ -111,7 +108,10 @@ public class QNodeDefinitionBuilder extends QItemDefinitionBuilder {
      *                               instance.
      */
     public QNodeDefinition build() throws IllegalStateException {
        return new QNodeDefinitionImpl(getName(), getDeclaringNodeType(), getAutoCreated(), getMandatory(), getOnParentVersion(), getProtected(), getDefaultPrimaryType(), getRequiredPrimaryTypes(), getAllowsSameNameSiblings());
        return new QNodeDefinitionImpl(getName(), getDeclaringNodeType(),
                getAutoCreated(), getMandatory(), getOnParentVersion(),
                getProtected(), getDefaultPrimaryType(),
                getRequiredPrimaryTypes(), getAllowsSameNameSiblings());
     }
 
 }
diff --git a/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/QPropertyDefinitionBuilder.java b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/QPropertyDefinitionBuilder.java
index 6311173fa..06a315b34 100644
-- a/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/QPropertyDefinitionBuilder.java
++ b/jackrabbit-spi-commons/src/main/java/org/apache/jackrabbit/spi/commons/nodetype/QPropertyDefinitionBuilder.java
@@ -21,6 +21,7 @@ import java.util.ArrayList;
 import java.util.Arrays;
 
 import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.PropertyType;
 
 import org.apache.jackrabbit.spi.QPropertyDefinition;
 import org.apache.jackrabbit.spi.QValue;
@@ -33,10 +34,10 @@ import org.apache.jackrabbit.spi.commons.QPropertyDefinitionImpl;
  */
 public class QPropertyDefinitionBuilder extends QItemDefinitionBuilder {
 
    private int requiredType;
    private int requiredType = PropertyType.UNDEFINED;
     private List<QValueConstraint> valueConstraints = new ArrayList<QValueConstraint>();
     private List<QValue> defaultValues;
    private boolean isMultiple;
    private boolean isMultiple = false;
     private boolean fullTextSearchable = true;
     private boolean queryOrderable = true;
     private String[] queryOperators = Operator.getAllQueryOperators();
diff --git a/jackrabbit-spi2jcr/src/main/java/org/apache/jackrabbit/spi2jcr/QNodeDefinitionImpl.java b/jackrabbit-spi2jcr/src/main/java/org/apache/jackrabbit/spi2jcr/QNodeDefinitionImpl.java
index 0bda75cb9..fd1d182d8 100644
-- a/jackrabbit-spi2jcr/src/main/java/org/apache/jackrabbit/spi2jcr/QNodeDefinitionImpl.java
++ b/jackrabbit-spi2jcr/src/main/java/org/apache/jackrabbit/spi2jcr/QNodeDefinitionImpl.java
@@ -18,6 +18,7 @@ package org.apache.jackrabbit.spi2jcr;
 
 import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
 import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
 import org.apache.jackrabbit.spi.Name;
 
 import javax.jcr.nodetype.NodeDefinition;
@@ -44,7 +45,7 @@ class QNodeDefinitionImpl
     QNodeDefinitionImpl(NodeDefinition nodeDef,
                         NamePathResolver resolver)
             throws NameException, NamespaceException {
        super(nodeDef.getName().equals(ANY_NAME.getLocalName()) ? ANY_NAME : resolver.getQName(nodeDef.getName()),
        super(nodeDef.getName().equals(NameConstants.ANY_NAME.getLocalName()) ? NameConstants.ANY_NAME : resolver.getQName(nodeDef.getName()),
                 nodeDef.getDeclaringNodeType() != null ? resolver.getQName(nodeDef.getDeclaringNodeType().getName()) : null,
                 nodeDef.isAutoCreated(), nodeDef.isMandatory(),
                 nodeDef.getOnParentVersion(), nodeDef.isProtected(),
diff --git a/jackrabbit-spi2jcr/src/main/java/org/apache/jackrabbit/spi2jcr/QPropertyDefinitionImpl.java b/jackrabbit-spi2jcr/src/main/java/org/apache/jackrabbit/spi2jcr/QPropertyDefinitionImpl.java
index 19ba1a418..d13edf8ce 100644
-- a/jackrabbit-spi2jcr/src/main/java/org/apache/jackrabbit/spi2jcr/QPropertyDefinitionImpl.java
++ b/jackrabbit-spi2jcr/src/main/java/org/apache/jackrabbit/spi2jcr/QPropertyDefinitionImpl.java
@@ -18,19 +18,15 @@ package org.apache.jackrabbit.spi2jcr;
 
 import org.apache.jackrabbit.spi.QValue;
 import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QValueConstraint;
 import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
 import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
 import org.apache.jackrabbit.spi.commons.value.ValueFormat;
 import org.apache.jackrabbit.spi.commons.nodetype.constraint.ValueConstraint;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
 
 import javax.jcr.nodetype.PropertyDefinition;
 import javax.jcr.RepositoryException;
 import javax.jcr.Value;
import javax.jcr.PropertyType;
import javax.jcr.NamespaceException;
 
 /**
  * <code>QPropertyDefinitionImpl</code> implements a property
@@ -53,7 +49,7 @@ class QPropertyDefinitionImpl
                             NamePathResolver resolver,
                             QValueFactory qValueFactory)
             throws RepositoryException, NameException {
        super(propDef.getName().equals(ANY_NAME.getLocalName()) ? ANY_NAME : resolver.getQName(propDef.getName()),
        super(propDef.getName().equals(NameConstants.ANY_NAME.getLocalName()) ? NameConstants.ANY_NAME : resolver.getQName(propDef.getName()),
                 resolver.getQName(propDef.getDeclaringNodeType().getName()),
                 propDef.isAutoCreated(),
                 propDef.isMandatory(),
@@ -91,21 +87,4 @@ class QPropertyDefinitionImpl
         }
         return defaultValues;
     }

    /**
     * Convert String jcr names to Name objects.
     *
     * @param aqos
     * @param resolver
     * @return
     * @throws NamespaceException
     * @throws IllegalNameException
     */
    private static Name[] convertQueryOperators(String[] aqos, NamePathResolver resolver) throws NamespaceException, IllegalNameException {
        Name[] names = new Name[aqos.length];
        for (int i = 0; i < aqos.length; i++) {
            names[i] = resolver.getQName(aqos[i]);
        }
        return names;
    }
 }
- 
2.19.1.windows.1

