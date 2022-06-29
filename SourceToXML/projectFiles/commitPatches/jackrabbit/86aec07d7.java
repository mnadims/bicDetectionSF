From 86aec07d7d8581c9c05642c52a00250708a06466 Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Fri, 2 Oct 2009 06:55:50 +0000
Subject: [PATCH] JCR-2336: Automatic type conversion no longer works

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@820908 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/jackrabbit/core/NodeImpl.java  | 596 ++++++------------
 .../apache/jackrabbit/core/PropertyImpl.java  |   3 +-
 .../apache/jackrabbit/core/NodeImplTest.java  |  52 ++
 3 files changed, 248 insertions(+), 403 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java
index b897277ea..87e291c05 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/NodeImpl.java
@@ -375,7 +375,7 @@ public class NodeImpl extends ItemImpl implements Node {
         if (isNew() && !hasProperty(name)) {
             // this is a new node and the property does not exist yet
             // -> no need to check item manager
            org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl def = getApplicablePropertyDefinition(
            PropertyDefinitionImpl def = getApplicablePropertyDefinition(
                     name, type, multiValued, exactTypeMatch);
             PropertyImpl prop = createChildProperty(name, type, def);
             status.set(CREATED);
@@ -412,7 +412,7 @@ public class NodeImpl extends ItemImpl implements Node {
         } catch (ItemNotFoundException e) {
             // does not exist yet:
             // find definition for the specified property and create property
            org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl def = getApplicablePropertyDefinition(
            PropertyDefinitionImpl def = getApplicablePropertyDefinition(
                     name, type, multiValued, exactTypeMatch);
             PropertyImpl prop = createChildProperty(name, type, def);
             status.set(CREATED);
@@ -420,14 +420,29 @@ public class NodeImpl extends ItemImpl implements Node {
         }
     }
 
    /**
     * Creates a new property with the given name and <code>type</code> hint and
     * property definition. If the given property definition is not of type
     * <code>UNDEFINED</code>, then it takes precendence over the
     * <code>type</code> hint.
     *
     * @param name the name of the property to create.
     * @param type the type hint.
     * @param def  the associated property definition.
     * @return the property instance.
     * @throws RepositoryException if the property cannot be created.
     */
     protected synchronized PropertyImpl createChildProperty(Name name, int type,
                                                            org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl def)
                                                            PropertyDefinitionImpl def)
             throws RepositoryException {
 
         // create a new property state
         PropertyState propState;
         try {
             QPropertyDefinition propDef = def.unwrap();
            if (def.getRequiredType() != PropertyType.UNDEFINED) {
                type = def.getRequiredType();
            }
             propState =
                     stateMgr.createTransientPropertyState(getNodeId(), name,
                             ItemState.STATUS_NEW);
@@ -583,7 +598,7 @@ public class NodeImpl extends ItemImpl implements Node {
     }
 
     protected void onRedefine(QNodeDefinition def) throws RepositoryException {
        org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl newDef =
        NodeDefinitionImpl newDef =
                 session.getNodeTypeManager().getNodeDefinition(def);
         // modify the state of 'this', i.e. the target node
         getOrCreateTransientItemState();
@@ -652,7 +667,7 @@ public class NodeImpl extends ItemImpl implements Node {
             prop = (PropertyImpl) itemMgr.getItem(new PropertyId(thisState.getNodeId(), NameConstants.JCR_MIXINTYPES));
         } else {
             // find definition for the jcr:mixinTypes property and create property
            org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl def = getApplicablePropertyDefinition(
            PropertyDefinitionImpl def = getApplicablePropertyDefinition(
                     NameConstants.JCR_MIXINTYPES, PropertyType.NAME, true, true);
             prop = createChildProperty(NameConstants.JCR_MIXINTYPES, PropertyType.NAME, def);
         }
@@ -715,7 +730,7 @@ public class NodeImpl extends ItemImpl implements Node {
      *                                      could be found
      * @throws RepositoryException          if another error occurs
      */
    protected org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl getApplicableChildNodeDefinition(Name nodeName,
    protected NodeDefinitionImpl getApplicableChildNodeDefinition(Name nodeName,
                                                                   Name nodeTypeName)
             throws ConstraintViolationException, RepositoryException {
         NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
@@ -737,7 +752,7 @@ public class NodeImpl extends ItemImpl implements Node {
      *                                      could be found
      * @throws RepositoryException          if another error occurs
      */
    protected org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl getApplicablePropertyDefinition(Name propertyName,
    protected PropertyDefinitionImpl getApplicablePropertyDefinition(Name propertyName,
                                                                      int type,
                                                                      boolean multiValued,
                                                                      boolean exactTypeMatch)
@@ -1044,7 +1059,7 @@ public class NodeImpl extends ItemImpl implements Node {
                             removeChildProperty(propName);
                             continue;
                         }
                        org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl pdi = getApplicablePropertyDefinition(
                        PropertyDefinitionImpl pdi = getApplicablePropertyDefinition(
                                 propName, propState.getType(),
                                 propState.isMultiValued(), false);
                         if (pdi.getRequiredType() != PropertyType.UNDEFINED
@@ -1107,7 +1122,7 @@ public class NodeImpl extends ItemImpl implements Node {
                             removeChildNode(entry.getName(), entry.getIndex());
                             continue;
                         }
                        org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl ndi = getApplicableChildNodeDefinition(
                        NodeDefinitionImpl ndi = getApplicableChildNodeDefinition(
                                 entry.getName(),
                                 nodeState.getNodeTypeName());
                         // redefine node
@@ -1459,7 +1474,7 @@ public class NodeImpl extends ItemImpl implements Node {
         }
 
         // Get the applicable child node definition for this node.
        org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl def;
        NodeDefinitionImpl def;
         try {
             def = getApplicableChildNodeDefinition(nodeName, nodeTypeName);
         } catch (RepositoryException e) {
@@ -1521,34 +1536,18 @@ public class NodeImpl extends ItemImpl implements Node {
     public PropertyImpl setProperty(Name name, Value[] values)
             throws ValueFormatException, VersionException, LockException,
             ConstraintViolationException, RepositoryException {

        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        int type;
        if (values == null || values.length == 0
                || values[0] == null) {
            type = PropertyType.UNDEFINED;
        } else {
            type = values[0].getType();
        }

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, true, false, status);
        try {
            prop.setValue(values);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
        int type = PropertyType.UNDEFINED;
        if (values != null) {
            for (Value v : values) {
                // use the type of the first value
                if (v != null) {
                    type = v.getType();
                    break;
                }
             }
            // rethrow
            throw re;
         }
        return prop;

        return setProperty(name, values, type, false);
     }
 
     /**
@@ -1569,30 +1568,7 @@ public class NodeImpl extends ItemImpl implements Node {
     public PropertyImpl setProperty(Name name, Value[] values, int type)
             throws ValueFormatException, VersionException, LockException,
             ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, true, true, status);
        try {
            if (prop.getDefinition().getRequiredType() == PropertyType.UNDEFINED
                    && type != PropertyType.UNDEFINED) {
                prop.setValue(ValueHelper.convert(values, type, session.getValueFactory()));
            } else {
                prop.setValue(values);
            }
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
        return setProperty(name, values, type, true);
     }
 
     /**
@@ -1612,30 +1588,7 @@ public class NodeImpl extends ItemImpl implements Node {
     public PropertyImpl setProperty(Name name, Value value)
             throws ValueFormatException, VersionException, LockException,
             ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        int type = PropertyType.UNDEFINED;
        if (value != null) {
            type = value.getType();
        }

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, false, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
        return setProperty(name, value, false);
     }
 
     /**
@@ -1898,7 +1851,7 @@ public class NodeImpl extends ItemImpl implements Node {
         session.getValidator().checkModify(this, options, Permission.NONE);
 
         // (4) check for name collisions
        org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl def;
        NodeDefinitionImpl def;
         try {
             def = getApplicableChildNodeDefinition(name, null);
         } catch (RepositoryException re) {
@@ -2155,38 +2108,7 @@ public class NodeImpl extends ItemImpl implements Node {
     public Property setProperty(String name, Value[] values)
             throws ValueFormatException, VersionException, LockException,
             ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        int type;
        if (values == null || values.length == 0
                || values[0] == null) {
            type = PropertyType.UNDEFINED;
        } else {
            type = values[0].getType();
        }

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, true, false, status);
        try {
            if (prop.getDefinition().getRequiredType() == PropertyType.UNDEFINED
                    && type != PropertyType.UNDEFINED) {
                prop.setValue(ValueHelper.convert(values, type, session.getValueFactory()));
            } else {
                prop.setValue(values);
            }
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
        return setProperty(session.getQName(name), values);
     }
 
     /**
@@ -2195,25 +2117,7 @@ public class NodeImpl extends ItemImpl implements Node {
     public Property setProperty(String name, Value[] values, int type)
             throws ValueFormatException, VersionException, LockException,
             ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, true, true, status);
        try {
            prop.setValue(values, type);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
        return setProperty(session.getQName(name), values, type);
     }
 
     /**
@@ -2222,30 +2126,11 @@ public class NodeImpl extends ItemImpl implements Node {
     public Property setProperty(String name, String[] values)
             throws ValueFormatException, VersionException, LockException,
             ConstraintViolationException, RepositoryException {
        /**
         * if the target property is not of type STRING then a
         * best-effort conversion is attempted
         */
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(
                name, PropertyType.STRING, true, false, status);
        try {
            prop.setValue(values);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        Value[] v = null;
        if (values != null) {
            v = ValueHelper.convert(values, PropertyType.STRING, session.getValueFactory());
         }
        return prop;
        return setProperty(name, v);
     }
 
     /**
@@ -2254,30 +2139,11 @@ public class NodeImpl extends ItemImpl implements Node {
     public Property setProperty(String name, String[] values, int type)
             throws ValueFormatException, VersionException, LockException,
             ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, true, true, status);
        try {
            if (prop.getDefinition().getRequiredType() == PropertyType.UNDEFINED
                    && type != PropertyType.UNDEFINED) {
                prop.setValue(ValueHelper.convert(values, type, session.getValueFactory()));
            } else {
                prop.setValue(values);
            }
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        Value[] v = null;
        if (values != null) {
            v = ValueHelper.convert(values, type, session.getValueFactory());
         }
        return prop;
        return setProperty(session.getQName(name), v, type, true);
     }
 
     /**
@@ -2286,26 +2152,11 @@ public class NodeImpl extends ItemImpl implements Node {
     public Property setProperty(String name, String value)
             throws ValueFormatException, VersionException, LockException,
             ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(
                name, PropertyType.STRING, false, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        Value v = null;
        if (value != null) {
            v = session.getValueFactory().createValue(value);
         }
        return prop;
        return setProperty(name, v);
     }
 
     /**
@@ -2314,30 +2165,11 @@ public class NodeImpl extends ItemImpl implements Node {
     public Property setProperty(String name, String value, int type)
             throws ValueFormatException, VersionException, LockException,
             ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, false, true, status);
        try {
            if (prop.getDefinition().getRequiredType() == PropertyType.UNDEFINED
                    && type != PropertyType.UNDEFINED) {
                prop.setValue(ValueHelper.convert(value, type, session.getValueFactory()));
            } else {
                prop.setValue(value);
            }
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        Value v = null;
        if (value != null) {
            v = session.getValueFactory().createValue(value, type);
         }
        return prop;
        return setProperty(session.getQName(name), v, true);
     }
 
     /**
@@ -2346,30 +2178,10 @@ public class NodeImpl extends ItemImpl implements Node {
     public Property setProperty(String name, Value value, int type)
             throws ValueFormatException, VersionException, LockException,
             ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, false, true, status);
        try {
            if (prop.getDefinition().getRequiredType() == PropertyType.UNDEFINED
                    && type != PropertyType.UNDEFINED) {
                prop.setValue(ValueHelper.convert(value, type, session.getValueFactory()));
            } else {
                prop.setValue(value);
            }
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        if (value != null) {
            value = ValueHelper.convert(value, type, session.getValueFactory());
         }
        return prop;
        return setProperty(session.getQName(name), value, true);
     }
 
     /**
@@ -2378,35 +2190,7 @@ public class NodeImpl extends ItemImpl implements Node {
     public Property setProperty(String name, Value value)
             throws ValueFormatException, VersionException, LockException,
             ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        int type = PropertyType.UNDEFINED;
        if (value != null) {
            type = value.getType();
        }

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, false, false, status);
        try {
            if (prop.getDefinition().getRequiredType() == PropertyType.UNDEFINED
                    && type != PropertyType.UNDEFINED) {
                prop.setValue(ValueHelper.convert(value, type, session.getValueFactory()));
            } else {
                prop.setValue(value);
            }
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
        return setProperty(session.getQName(name), value);
     }
 
     /**
@@ -2415,26 +2199,11 @@ public class NodeImpl extends ItemImpl implements Node {
     public Property setProperty(String name, InputStream value)
             throws ValueFormatException, VersionException, LockException,
             ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(
                name, PropertyType.BINARY, false, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        Value v = null;
        if (value != null) {
            v = session.getValueFactory().createValue(value);
         }
        return prop;
        return setProperty(name, v);
     }
 
     /**
@@ -2443,26 +2212,8 @@ public class NodeImpl extends ItemImpl implements Node {
     public Property setProperty(String name, boolean value)
             throws ValueFormatException, VersionException, LockException,
             ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(
                name, PropertyType.BOOLEAN, false, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
        Value v = session.getValueFactory().createValue(value);
        return setProperty(name, v);
     }
 
     /**
@@ -2471,26 +2222,8 @@ public class NodeImpl extends ItemImpl implements Node {
     public Property setProperty(String name, double value)
             throws ValueFormatException, VersionException, LockException,
             ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(
                name, PropertyType.DOUBLE, false, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
        Value v = session.getValueFactory().createValue(value);
        return setProperty(name, v);
     }
 
     /**
@@ -2499,43 +2232,98 @@ public class NodeImpl extends ItemImpl implements Node {
     public Property setProperty(String name, long value)
             throws ValueFormatException, VersionException, LockException,
             ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();
        Value v = session.getValueFactory().createValue(value);
        return setProperty(name, v);
    }
 
        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(
                name, PropertyType.LONG, false, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
    /**
     * {@inheritDoc}
     */
    public Property setProperty(String name, Calendar value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        Value v = null;
        if (value != null) {
            try {
                v = session.getValueFactory().createValue(value);
            } catch (IllegalArgumentException e) {
                // thrown if calendar cannot be formatted as ISO8601
                throw new ValueFormatException(e.getMessage());
             }
            // rethrow
            throw re;
         }
        return prop;
        return setProperty(name, v);
     }
 
     /**
      * {@inheritDoc}
      */
    public Property setProperty(String name, Calendar value)
    public Property setProperty(String name, Node value)
             throws ValueFormatException, VersionException, LockException,
             ConstraintViolationException, RepositoryException {
        Value v = null;
        if (value != null) {
            try {
                v = session.getValueFactory().createValue(value);
            } catch (UnsupportedRepositoryOperationException e) {
                // happens when node is not referenceable
                throw new ValueFormatException("node is not of type mix:referenceable");
            }
        }
        return setProperty(name, v);
    }

    /**
     * Implementation for <code>setProperty()</code> using a single {@link
     * Value}. The type of the returned property is enforced based on the
     * <code>enforceType</code> flag. If set to <code>true</code>, the returned
     * property is of the passed type if it didn't exist before. If set to
     * <code>false</code>, then the returned property may be of some other type,
     * but still must be based on an existing property definition for the given
     * name and single-valued flag. The resulting type is taken from that
     * definition and the implementation tries to convert the passed value to
     * that type. If that fails, then a {@link ValueFormatException} is thrown.
     *
     * @param name        the name of the property to set.
     * @param value       the value to set. If <code>null</code> the property is
     *                    removed.
     * @param enforceType if the type of <code>value</code> is enforced.
     * @return the <code>Property</code> object set, or <code>null</code> if
     *         this method was used to remove a property (by setting its value
     *         to <code>null</code>).
     * @throws ValueFormatException         if <code>value</code> cannot be
     *                                      converted to the specified type or
     *                                      if the property already exists and
     *                                      is multi-valued.
     * @throws VersionException             if this node is read-only due to a
     *                                      checked-in node and this implementation
     *                                      performs this validation immediately.
     * @throws LockException                if a lock prevents the setting of
     *                                      the property and this implementation
     *                                      performs this validation immediately.
     * @throws ConstraintViolationException if the change would violate a
     *                                      node-type or other constraint and
     *                                      this implementation performs this
     *                                      validation immediately.
     * @throws RepositoryException          if another error occurs.
     */
    protected PropertyImpl setProperty(Name name,
                                       Value value,
                                       boolean enforceType) throws
            ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
         // check state of this instance
         sanityCheck();
 
         // check pre-conditions for setting property
         checkSetProperty();
 
        int type = PropertyType.UNDEFINED;
        if (value != null) {
            type = value.getType();
        }

         BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(
                name, PropertyType.DATE, false, false, status);
        PropertyImpl prop = getOrCreateProperty(name, type, false, enforceType, status);
         try {
             prop.setValue(value);
         } catch (RepositoryException re) {
@@ -2550,10 +2338,45 @@ public class NodeImpl extends ItemImpl implements Node {
     }
 
     /**
     * {@inheritDoc}
     */
    public Property setProperty(String name, Node value)
            throws ValueFormatException, VersionException, LockException,
     * Implementation for <code>setProperty()</code> using a {@link Value}
     * array. The type of the returned property is enforced based on the
     * <code>enforceType</code> flag. If set to <code>true</code>, the returned
     * property is of the passed type if it didn't exist before. If set to
     * <code>false</code>, then the returned property may be of some other type,
     * but still must be based on an existing property definition for the given
     * name and multi-valued flag. The resulting type is taken from that
     * definition and the implementation tries to convert the passed values to
     * that type. If that fails, then a {@link ValueFormatException} is thrown.
     *
     * @param name        the name of the property to set.
     * @param values      the values to set. If <code>null</code> the property
     *                    is removed.
     * @param type        the target type of the values to set.
     * @param enforceType if the target type is enforced.
     * @return the <code>Property</code> object set, or <code>null</code> if
     *         this method was used to remove a property (by setting its value
     *         to <code>null</code>).
     * @throws ValueFormatException         if a value cannot be converted to
     *                                      the specified type or if the
     *                                      property already exists and is not
     *                                      multi-valued.
     * @throws VersionException             if this node is read-only due to a
     *                                      checked-in node and this implementation
     *                                      performs this validation immediately.
     * @throws LockException                if a lock prevents the setting of
     *                                      the property and this implementation
     *                                      performs this validation immediately.
     * @throws ConstraintViolationException if the change would violate a
     *                                      node-type or other constraint and
     *                                      this implementation performs this
     *                                      validation immediately.
     * @throws RepositoryException          if another error occurs.
     */
    protected PropertyImpl setProperty(Name name,
                                       Value[] values,
                                       int type,
                                       boolean enforceType) throws
            ValueFormatException, VersionException, LockException,
             ConstraintViolationException, RepositoryException {
         // check state of this instance
         sanityCheck();
@@ -2562,10 +2385,9 @@ public class NodeImpl extends ItemImpl implements Node {
         checkSetProperty();
 
         BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(
                name, PropertyType.REFERENCE, false, true, status);
        PropertyImpl prop = getOrCreateProperty(name, type, true, enforceType, status);
         try {
            prop.setValue(value);
            prop.setValue(values, type);
         } catch (RepositoryException re) {
             if (status.get(CREATED)) {
                 // setting value failed, get rid of newly created property
@@ -3750,7 +3572,7 @@ public class NodeImpl extends ItemImpl implements Node {
                             removeChildProperty(propName);
                             continue;
                         }
                        org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl pdi = getApplicablePropertyDefinition(
                        PropertyDefinitionImpl pdi = getApplicablePropertyDefinition(
                                 propName, propState.getType(),
                                 propState.isMultiValued(), false);
                         if (pdi.getRequiredType() != PropertyType.UNDEFINED
@@ -3818,7 +3640,7 @@ public class NodeImpl extends ItemImpl implements Node {
                             removeChildNode(entry.getName(), entry.getIndex());
                             continue;
                         }
                        org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl ndi = getApplicableChildNodeDefinition(
                        NodeDefinitionImpl ndi = getApplicableChildNodeDefinition(
                                 entry.getName(),
                                 nodeState.getNodeTypeName());
                         // redefine node
@@ -3859,26 +3681,11 @@ public class NodeImpl extends ItemImpl implements Node {
     public Property setProperty(String name, BigDecimal value)
             throws ValueFormatException, VersionException, LockException,
             ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(
                name, PropertyType.DECIMAL, false, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        Value v = null;
        if (value != null) {
            v = session.getValueFactory().createValue(value);
         }
        return prop;
        return setProperty(name, v);
     }
 
     /**
@@ -3887,26 +3694,11 @@ public class NodeImpl extends ItemImpl implements Node {
     public Property setProperty(String name, Binary value)
             throws ValueFormatException, VersionException, LockException,
             ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(
                name, PropertyType.BINARY, false, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        Value v = null;
        if (value != null) {
            v = session.getValueFactory().createValue(value);
         }
        return prop;
        return setProperty(name, v);
     }
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/PropertyImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/PropertyImpl.java
index 81f0a9fd8..12d49fdd9 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/PropertyImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/PropertyImpl.java
@@ -48,6 +48,7 @@ import org.apache.jackrabbit.spi.Path;
 import org.apache.jackrabbit.spi.QPropertyDefinition;
 import org.apache.jackrabbit.spi.commons.name.NameConstants;
 import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl;
 import org.apache.jackrabbit.value.ValueHelper;
 import org.apache.commons.io.input.AutoCloseInputStream;
 import org.slf4j.Logger;
@@ -166,7 +167,7 @@ public class PropertyImpl extends ItemImpl implements Property {
     }
 
     protected void onRedefine(QPropertyDefinition def) throws RepositoryException {
        org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl newDef =
        PropertyDefinitionImpl newDef =
                 session.getNodeTypeManager().getPropertyDefinition(def);
         data.setDefinition(newDef);
     }
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NodeImplTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NodeImplTest.java
index 9e46afe36..fcca3dce2 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NodeImplTest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/NodeImplTest.java
@@ -19,6 +19,7 @@ package org.apache.jackrabbit.core;
 import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
 import org.apache.jackrabbit.test.AbstractJCRTest;
 import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.api.nodetype.NodeTypeUtil;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -26,7 +27,10 @@ import javax.jcr.ItemExistsException;
 import javax.jcr.Node;
 import javax.jcr.RepositoryException;
 import javax.jcr.Session;
import javax.jcr.Property;
import javax.jcr.PropertyType;
 import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
 import javax.jcr.security.AccessControlManager;
 import javax.jcr.security.AccessControlPolicy;
 import javax.jcr.security.AccessControlPolicyIterator;
@@ -34,6 +38,7 @@ import javax.jcr.security.Privilege;
 import java.security.Principal;
 import java.security.acl.Group;
 import java.util.Iterator;
import java.util.Calendar;
 
 /** <code>NodeImplTest</code>... */
 public class NodeImplTest extends AbstractJCRTest {
@@ -153,4 +158,51 @@ public class NodeImplTest extends AbstractJCRTest {
         }
     }
 
    /**
     * Test case for JCR-2336. Setting jcr:data (of type BINARY) must convert
     * the String value to a binary.
     *
     * @throws RepositoryException -
     */
    public void testSetPropertyConvertValue() throws RepositoryException {
        Node content = testRootNode.addNode("jcr:content", "nt:resource");
        content.setProperty("jcr:lastModified", Calendar.getInstance());
        content.setProperty("jcr:mimeType", "text/plain");
        content.setProperty("jcr:data", "Hello");
        superuser.save();
    }

    public void testSetPropertyConvertToString() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, "nt:folder");
        n.addMixin("mix:title");
        // must convert to string there is no other definition for this property
        Property p = n.setProperty("jcr:title", 123);
        assertEquals(PropertyType.nameFromValue(PropertyType.STRING),
                PropertyType.nameFromValue(p.getType()));
    }

    public void testSetPropertyExplicitType() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, ntUnstructured);
        n.addMixin("mix:title");
        Property p = n.setProperty("jcr:title", "foo");
        assertEquals(PropertyType.nameFromValue(PropertyType.STRING),
                PropertyType.nameFromValue(p.getType()));
        assertEquals(PropertyType.nameFromValue(PropertyType.STRING),
                PropertyType.nameFromValue(p.getDefinition().getRequiredType()));
        p.remove();
        // must use residual definition from nt:unstructured
        p = n.setProperty("jcr:title", 123);
        assertEquals(PropertyType.nameFromValue(PropertyType.LONG),
                PropertyType.nameFromValue(p.getType()));
        assertEquals(PropertyType.nameFromValue(PropertyType.UNDEFINED),
                PropertyType.nameFromValue(p.getDefinition().getRequiredType()));
    }

    public void testSetPropertyConvertMultiValued() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, "test:canSetProperty");
        // must convert to long there is no other definition for this property
        Property p = n.setProperty("LongMultiple", new String[]{"123", "456"});
        assertEquals(PropertyType.nameFromValue(PropertyType.LONG),
                PropertyType.nameFromValue(p.getType()));
    }
 }
- 
2.19.1.windows.1

