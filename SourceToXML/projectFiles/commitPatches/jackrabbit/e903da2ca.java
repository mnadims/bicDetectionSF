From e903da2ca8f8635de1526e82bb5e1156204620c6 Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Mon, 23 Feb 2009 10:04:53 +0000
Subject: [PATCH] JCR-1990: Optimize queries with relative path in order by
 clause

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@746946 13f79535-47bb-0310-9956-ffa450edef68
--
 .../lucene/AbstractNamespaceMappings.java     |  72 +++++++
 .../core/query/lucene/AggregateRule.java      |  14 ++
 .../core/query/lucene/AggregateRuleImpl.java  | 194 ++++++++++++++----
 .../core/query/lucene/ComparableBoolean.java  |  43 ++++
 .../lucene/FileBasedNamespaceMappings.java    |  36 +---
 .../core/query/lucene/IndexFormatVersion.java |  12 ++
 .../IndexingConfigurationEntityResolver.java  |   3 +
 .../lucene/IndexingConfigurationImpl.java     |   2 +-
 .../query/lucene/JQOM2LuceneQueryBuilder.java |   4 +-
 .../core/query/lucene/LuceneQueryBuilder.java |   6 +-
 .../NSRegistryBasedNamespaceMappings.java     |  29 +--
 .../core/query/lucene/NameQuery.java          |   2 +-
 .../core/query/lucene/NameRangeQuery.java     |   4 +-
 .../core/query/lucene/NamespaceMappings.java  |  20 +-
 .../core/query/lucene/SearchIndex.java        | 118 +++++++++--
 .../core/query/lucene/SharedFieldCache.java   | 127 ++++++++----
 .../lucene/SharedFieldSortComparator.java     |  76 +++----
 .../query/lucene/SingletonTokenStream.java    |   9 +
 .../lucene/indexing-configuration-1.2.dtd     |  93 +++++++++
 .../core/query/IndexingAggregateTest.java     |  98 +++++++++
 .../jackrabbit/core/query/OrderByTest.java    |  91 ++++----
 .../default/indexing-configuration.xml        |  10 +
 .../workspaces/default/workspace.xml          |   1 +
 .../indexing-test/indexing-configuration.xml  |   7 +-
 24 files changed, 809 insertions(+), 262 deletions(-)
 create mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractNamespaceMappings.java
 create mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ComparableBoolean.java
 create mode 100644 jackrabbit-core/src/main/resources/org/apache/jackrabbit/core/query/lucene/indexing-configuration-1.2.dtd
 create mode 100644 jackrabbit-core/src/test/repository/workspaces/default/indexing-configuration.xml

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractNamespaceMappings.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractNamespaceMappings.java
new file mode 100644
index 000000000..00a0e79de
-- /dev/null
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractNamespaceMappings.java
@@ -0,0 +1,72 @@
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
package org.apache.jackrabbit.core.query.lucene;

import javax.jcr.NamespaceException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

/**
 * <code>AbstractNamespaceMappings</code> is the base class for index internal
 * namespace mappings.
 */
public abstract class AbstractNamespaceMappings
        implements NamespaceMappings, NamespaceResolver {

    /**
     * The name resolver used to translate the qualified name to JCR name
     */
    private final NamePathResolver resolver;

    public AbstractNamespaceMappings() {
        this.resolver = NamePathResolverImpl.create(this);
    }

    //----------------------------< NamespaceMappings >-------------------------

    /**
     * {@inheritDoc}
     */
    public String translateName(Name qName)
            throws IllegalNameException {
        try {
            return resolver.getJCRName(qName);
        } catch (NamespaceException e) {
            // should never happen actually, because we create yet unknown
            // uri mappings on the fly.
            throw new IllegalNameException("Internal error.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String translatePath(Path path) throws IllegalNameException {
        try {
            return resolver.getJCRPath(path);
        } catch (NamespaceException e) {
            // should never happen actually, because we create yet unknown
            // uri mappings on the fly.
            throw new IllegalNameException("Internal error.", e);
        }
    }

}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AggregateRule.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AggregateRule.java
index e1fc78635..187e20157 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AggregateRule.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AggregateRule.java
@@ -18,6 +18,7 @@ package org.apache.jackrabbit.core.query.lucene;
 
 import org.apache.jackrabbit.core.state.NodeState;
 import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PropertyState;
 
 import javax.jcr.RepositoryException;
 
@@ -55,4 +56,17 @@ public interface AggregateRule {
      */
     NodeState[] getAggregatedNodeStates(NodeState nodeState)
             throws ItemStateException;

    /**
     * Returns the property states that are part of the indexing aggregate of
     * the <code>nodeState</code>.
     *
     * @param nodeState a node state
     * @return the property states that are part of the indexing aggregate of
     *         <code>nodeState</code>. Returns <code>null</code> if this
     *         aggregate does not apply to <code>nodeState</code>.
     * @throws ItemStateException if an error occurs.
     */
    public PropertyState[] getAggregatedPropertyStates(NodeState nodeState)
            throws ItemStateException;
 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AggregateRuleImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AggregateRuleImpl.java
index 368b438b3..b4a1adbfe 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AggregateRuleImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AggregateRuleImpl.java
@@ -27,8 +27,10 @@ import org.apache.jackrabbit.core.state.NodeState;
 import org.apache.jackrabbit.core.state.ItemStateManager;
 import org.apache.jackrabbit.core.state.ItemStateException;
 import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.PropertyState;
 import org.apache.jackrabbit.core.HierarchyManager;
 import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
 import org.apache.jackrabbit.util.Text;
 import org.w3c.dom.Node;
 import org.w3c.dom.NodeList;
@@ -36,6 +38,8 @@ import org.w3c.dom.CharacterData;
 
 import javax.jcr.RepositoryException;
 import javax.jcr.NamespaceException;
import javax.jcr.PathNotFoundException;

 import java.util.ArrayList;
 import java.util.List;
 import java.util.Arrays;
@@ -60,9 +64,14 @@ class AggregateRuleImpl implements AggregateRule {
     private final Name nodeTypeName;
 
     /**
     * The rules that define this indexing aggregate.
     * The node includes of this indexing aggregate.
     */
    private final NodeInclude[] nodeIncludes;

    /**
     * The property includes of this indexing aggregate.
      */
    private final Rule[] rules;
    private final PropertyInclude[] propertyIncludes;
 
     /**
      * The item state manager to retrieve additional item states.
@@ -91,11 +100,12 @@ class AggregateRuleImpl implements AggregateRule {
     AggregateRuleImpl(Node config,
                       NameResolver resolver,
                       ItemStateManager ism,
                      HierarchyManager hmgr)
            throws MalformedPathException, IllegalNameException, NamespaceException {
                      HierarchyManager hmgr) throws MalformedPathException,
            IllegalNameException, NamespaceException, PathNotFoundException {
         this.resolver = resolver;
         this.nodeTypeName = getNodeTypeName(config);
        this.rules = getRules(config);
        this.nodeIncludes = getNodeIncludes(config);
        this.propertyIncludes = getPropertyIncludes(config);
         this.ism = ism;
         this.hmgr = hmgr;
     }
@@ -104,7 +114,7 @@ class AggregateRuleImpl implements AggregateRule {
      * Returns root node state for the indexing aggregate where
      * <code>nodeState</code> belongs to.
      *
     * @param nodeState
     * @param nodeState the node state.
      * @return the root node state of the indexing aggregate or
      *         <code>null</code> if <code>nodeState</code> does not belong to an
      *         indexing aggregate.
@@ -113,8 +123,16 @@ class AggregateRuleImpl implements AggregateRule {
      */
     public NodeState getAggregateRoot(NodeState nodeState)
             throws ItemStateException, RepositoryException {
        for (int i = 0; i < rules.length; i++) {
            NodeState aggregateRoot = rules[i].matches(nodeState);
        for (int i = 0; i < nodeIncludes.length; i++) {
            NodeState aggregateRoot = nodeIncludes[i].matches(nodeState);
            if (aggregateRoot != null
                    && aggregateRoot.getNodeTypeName().equals(nodeTypeName)) {
                return aggregateRoot;
            }
        }
        // check property includes
        for (int i = 0; i < propertyIncludes.length; i++) {
            NodeState aggregateRoot = propertyIncludes[i].matches(nodeState);
             if (aggregateRoot != null
                     && aggregateRoot.getNodeTypeName().equals(nodeTypeName)) {
                 return aggregateRoot;
@@ -137,8 +155,8 @@ class AggregateRuleImpl implements AggregateRule {
             throws ItemStateException {
         if (nodeState.getNodeTypeName().equals(nodeTypeName)) {
             List nodeStates = new ArrayList();
            for (int i = 0; i < rules.length; i++) {
                nodeStates.addAll(Arrays.asList(rules[i].resolve(nodeState)));
            for (int i = 0; i < nodeIncludes.length; i++) {
                nodeStates.addAll(Arrays.asList(nodeIncludes[i].resolve(nodeState)));
             }
             if (nodeStates.size() > 0) {
                 return (NodeState[]) nodeStates.toArray(new NodeState[nodeStates.size()]);
@@ -147,6 +165,25 @@ class AggregateRuleImpl implements AggregateRule {
         return null;
     }
 
    /**
     * {@inheritDoc}
     */
    public PropertyState[] getAggregatedPropertyStates(NodeState nodeState)
            throws ItemStateException {
        if (nodeState.getNodeTypeName().equals(nodeTypeName)) {
            List propStates = new ArrayList();
            for (int i = 0; i < propertyIncludes.length; i++) {
                propStates.addAll(Arrays.asList(
                        propertyIncludes[i].resolvePropertyStates(nodeState)));
            }
            if (propStates.size() > 0) {
                return (PropertyState[]) propStates.toArray(
                        new PropertyState[propStates.size()]);
            }
        }
        return null;
    }

     //---------------------------< internal >-----------------------------------
 
     /**
@@ -166,10 +203,10 @@ class AggregateRuleImpl implements AggregateRule {
     }
 
     /**
     * Creates rules defined in the <code>config</code>.
     * Creates node includes defined in the <code>config</code>.
      *
      * @param config the indexing aggregate configuration.
     * @return the rules defined in the <code>config</code>.
     * @return the node includes defined in the <code>config</code>.
      * @throws MalformedPathException if a path in the configuration is
      *                                malformed.
      * @throws IllegalNameException   if the node type name contains illegal
@@ -177,9 +214,9 @@ class AggregateRuleImpl implements AggregateRule {
      * @throws NamespaceException if the node type contains an unknown
      *                                prefix.
      */
    private Rule[] getRules(Node config)
    private NodeInclude[] getNodeIncludes(Node config)
             throws MalformedPathException, IllegalNameException, NamespaceException {
        List rules = new ArrayList();
        List includes = new ArrayList();
         NodeList childNodes = config.getChildNodes();
         for (int i = 0; i < childNodes.getLength(); i++) {
             Node n = childNodes.item(i);
@@ -198,10 +235,44 @@ class AggregateRuleImpl implements AggregateRule {
                         builder.addLast(resolver.getQName(elements[j]));
                     }
                 }
                rules.add(new Rule(builder.getPath(), ntName));
                includes.add(new NodeInclude(builder.getPath(), ntName));
             }
         }
        return (Rule[]) rules.toArray(new Rule[rules.size()]);
        return (NodeInclude[]) includes.toArray(new NodeInclude[includes.size()]);
    }

    /**
     * Creates property includes defined in the <code>config</code>.
     *
     * @param config the indexing aggregate configuration.
     * @return the property includes defined in the <code>config</code>.
     * @throws MalformedPathException if a path in the configuration is
     *                                malformed.
     * @throws IllegalNameException   if the node type name contains illegal
     *                                characters.
     * @throws NamespaceException if the node type contains an unknown
     *                                prefix.
     */
    private PropertyInclude[] getPropertyIncludes(Node config) throws
            MalformedPathException, IllegalNameException, NamespaceException,
            PathNotFoundException {
        List includes = new ArrayList();
        NodeList childNodes = config.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node n = childNodes.item(i);
            if (n.getNodeName().equals("include-property")) {
                String[] elements = Text.explode(getTextContent(n), '/');
                PathBuilder builder = new PathBuilder();
                for (int j = 0; j < elements.length; j++) {
                    if (elements[j].equals("*")) {
                        throw new IllegalNameException("* not supported in include-property");
                    }
                    builder.addLast(resolver.getQName(elements[j]));
                }
                includes.add(new PropertyInclude(builder.getPath()));
            }
        }
        return (PropertyInclude[]) includes.toArray(new PropertyInclude[includes.size()]);
     }
 
     //---------------------------< internal >-----------------------------------
@@ -222,17 +293,17 @@ class AggregateRuleImpl implements AggregateRule {
         return content.toString();
     }
 
    private final class Rule {
    private abstract class AbstractInclude {
 
         /**
          * Optional node type name.
          */
        private final Name nodeTypeName;
        protected final Name nodeTypeName;
 
         /**
          * A relative path pattern.
          */
        private final Path pattern;
        protected final Path pattern;
 
         /**
          * Creates a new rule with a relative path pattern and an optional node
@@ -242,7 +313,7 @@ class AggregateRuleImpl implements AggregateRule {
          *                     types are allowed.
          * @param pattern      a relative path pattern.
          */
        private Rule(Path pattern, Name nodeTypeName) {
        AbstractInclude(Path pattern, Name nodeTypeName) {
             this.nodeTypeName = nodeTypeName;
             this.pattern = pattern;
         }
@@ -255,6 +326,9 @@ class AggregateRuleImpl implements AggregateRule {
          * @return the root node state of the indexing aggregate or
          *         <code>null</code> if <code>nodeState</code> does not belong
          *         to an indexing aggregate defined by this rule.
         * @throws ItemStateException if an error occurs while accessing node
         *                            states.
         * @throws RepositoryException if another error occurs.
          */
         NodeState matches(NodeState nodeState)
                 throws ItemStateException, RepositoryException {
@@ -290,20 +364,6 @@ class AggregateRuleImpl implements AggregateRule {
             return null;
         }
 
        /**
         * Resolves the <code>nodeState</code> using this rule.
         *
         * @param nodeState the root node of the enclosing indexing aggregate.
         * @return the descendant node states as defined by this rule.
         * @throws ItemStateException if an error occurs while resolving the
         *                            node states.
         */
        NodeState[] resolve(NodeState nodeState) throws ItemStateException {
            List nodeStates = new ArrayList();
            resolve(nodeState, nodeStates, 0);
            return (NodeState[]) nodeStates.toArray(new NodeState[nodeStates.size()]);
        }

         //-----------------------------< internal >-----------------------------
 
         /**
@@ -316,7 +376,7 @@ class AggregateRuleImpl implements AggregateRule {
          * @throws ItemStateException if an error occurs while accessing node
          *                            states.
          */
        private void resolve(NodeState nodeState, List collector, int offset)
        protected void resolve(NodeState nodeState, List collector, int offset)
                 throws ItemStateException {
             Name currentName = pattern.getElements()[offset].getName();
             List cne;
@@ -347,4 +407,68 @@ class AggregateRuleImpl implements AggregateRule {
             }
         }
     }

    private final class NodeInclude extends AbstractInclude {

        /**
         * Creates a new node include with a relative path pattern and an
         * optional node type name.
         *
         * @param nodeTypeName node type name or <code>null</code> if all node
         *                     types are allowed.
         * @param pattern      a relative path pattern.
         */
        NodeInclude(Path pattern, Name nodeTypeName) {
            super(pattern, nodeTypeName);
        }

        /**
         * Resolves the <code>nodeState</code> using this rule.
         *
         * @param nodeState the root node of the enclosing indexing aggregate.
         * @return the descendant node states as defined by this rule.
         * @throws ItemStateException if an error occurs while resolving the
         *                            node states.
         */
        NodeState[] resolve(NodeState nodeState) throws ItemStateException {
            List nodeStates = new ArrayList();
            resolve(nodeState, nodeStates, 0);
            return (NodeState[]) nodeStates.toArray(new NodeState[nodeStates.size()]);
        }
    }

    private final class PropertyInclude extends AbstractInclude {

        private final Name propertyName;

        PropertyInclude(Path pattern)
                throws PathNotFoundException {
            super(pattern.getAncestor(1), null);
            this.propertyName = pattern.getNameElement().getName();
        }

        /**
         * Resolves the <code>nodeState</code> using this rule.
         *
         * @param nodeState the root node of the enclosing indexing aggregate.
         * @return the descendant property states as defined by this rule.
         * @throws ItemStateException if an error occurs while resolving the
         *                            property states.
         */
        PropertyState[] resolvePropertyStates(NodeState nodeState)
                throws ItemStateException {
            List nodeStates = new ArrayList();
            resolve(nodeState, nodeStates, 0);
            List propStates = new ArrayList();
            for (Iterator it = nodeStates.iterator(); it.hasNext(); ) {
                NodeState state = (NodeState) it.next();
                if (state.hasPropertyName(propertyName)) {
                    PropertyId propId = new PropertyId(state.getNodeId(), propertyName);
                    propStates.add(ism.getItemState(propId));
                }
            }
            return (PropertyState[]) propStates.toArray(
                    new PropertyState[propStates.size()]);
        }
    }
 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ComparableBoolean.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ComparableBoolean.java
new file mode 100644
index 000000000..7e84aa46e
-- /dev/null
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ComparableBoolean.java
@@ -0,0 +1,43 @@
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
package org.apache.jackrabbit.core.query.lucene;

/**
 * Represents a boolean that implement {@link Comparable}. This class can
 * be removed when we move to Java 5.
 */
public final class ComparableBoolean implements Comparable {

    private static final ComparableBoolean TRUE = new ComparableBoolean(true);

    private static final ComparableBoolean FALSE = new ComparableBoolean(false);

    private final boolean value;

    private ComparableBoolean(boolean value) {
        this.value = value;
    }

    public int compareTo(Object o) {
        ComparableBoolean b = (ComparableBoolean) o;
        return (b.value == value ? 0 : (value ? 1 : -1));
    }

    public static ComparableBoolean valueOf(boolean value) {
        return value ? TRUE : FALSE;
    }
}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FileBasedNamespaceMappings.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FileBasedNamespaceMappings.java
index c913e684e..fe969d01f 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FileBasedNamespaceMappings.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FileBasedNamespaceMappings.java
@@ -16,10 +16,7 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
 import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.Name;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -37,8 +34,8 @@ import java.util.Map;
 import java.util.Properties;
 
 /**
 * The class <code>NamespaceMappings</code> implements a {@link
 * org.apache.jackrabbit.core.NamespaceResolver} that holds a namespace
 * The class <code>NamespaceMappings</code> implements a
 * {@link NamespaceResolver} that holds a namespace
  * mapping that is used internally in the search index. Storing paths with the
  * full uri of a namespace would require too much space in the search index.
  * <p/>
@@ -46,8 +43,7 @@ import java.util.Properties;
  * prefix is created on the fly and associated with the namespace. Known
  * namespace mappings are stored in a properties file.
  */
public class FileBasedNamespaceMappings
        implements NamespaceResolver, NamespaceMappings {
public class FileBasedNamespaceMappings extends AbstractNamespaceMappings {
 
     /**
      * Default logger instance for this class
@@ -59,11 +55,6 @@ public class FileBasedNamespaceMappings
      */
     private final File storage;
 
    /**
     * The name resolver used to translate the qualified name to JCR name
     */
    private final NameResolver nameResolver;

     /**
      * Map of uris indexed by prefixes
      */
@@ -90,7 +81,6 @@ public class FileBasedNamespaceMappings
     public FileBasedNamespaceMappings(File file) throws IOException {
         storage = file;
         load();
        nameResolver = NamePathResolverImpl.create(this);
     }
 
     /**
@@ -138,26 +128,6 @@ public class FileBasedNamespaceMappings
         return prefix;
     }
 
    //----------------------------< NamespaceMappings >-------------------------

    /**
     * Translates a property name from a session local namespace mapping
     * into a search index private namespace mapping.
     *
     * @param qName     the property name to translate
     * @return the translated property name
     */
    public String translatePropertyName(Name qName)
            throws IllegalNameException {
        try {
            return nameResolver.getJCRName(qName);
        } catch (NamespaceException e) {
            // should never happen actually, because we create yet unknown
            // uri mappings on the fly.
            throw new IllegalNameException("Internal error.", e);
        }
    }

     //-----------------------< internal >---------------------------------------
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexFormatVersion.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexFormatVersion.java
index c4791c8f7..b55625c40 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexFormatVersion.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexFormatVersion.java
@@ -78,6 +78,18 @@ public class IndexFormatVersion {
         return version;
     }
 
    /**
     * Returns <code>true</code> if this version is at least as high as the
     * given <code>version</code>.
     *
     * @param version the other version to compare.
     * @return <code>true</code> if this version is at least as high as the
     *         provided; <code>false</code> otherwise.
     */
    public boolean isAtLeast(IndexFormatVersion version) {
        return this.version >= version.getVersion();
    }

     /**
      * @return a string representation of this index format version.
      */
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingConfigurationEntityResolver.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingConfigurationEntityResolver.java
index 83976b7ec..295eed317 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingConfigurationEntityResolver.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingConfigurationEntityResolver.java
@@ -45,6 +45,9 @@ public class IndexingConfigurationEntityResolver implements EntityResolver {
         systemIds.put(
                 "http://jackrabbit.apache.org/dtd/indexing-configuration-1.1.dtd",
                 "indexing-configuration-1.1.dtd");
        systemIds.put(
                "http://jackrabbit.apache.org/dtd/indexing-configuration-1.2.dtd",
                "indexing-configuration-1.2.dtd");
         SYSTEM_IDS = Collections.unmodifiableMap(systemIds);
     }
 
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingConfigurationImpl.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingConfigurationImpl.java
index 16862aab5..de268b469 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingConfigurationImpl.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/IndexingConfigurationImpl.java
@@ -162,7 +162,7 @@ public class IndexingConfigurationImpl implements IndexingConfiguration {
                                     if (propertyNode.getNodeName().equals("property")) {
                                         // get property name
                                         Name propName = resolver.getQName(getTextContent(propertyNode));
                                        String fieldName = nsMappings.translatePropertyName(propName);
                                        String fieldName = nsMappings.translateName(propName);
                                         // set analyzer for the fulltext property fieldname
                                         int idx = fieldName.indexOf(':');
                                         fieldName = fieldName.substring(0, idx + 1)
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JQOM2LuceneQueryBuilder.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JQOM2LuceneQueryBuilder.java
index 3e872c31a..ad6c65d72 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JQOM2LuceneQueryBuilder.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JQOM2LuceneQueryBuilder.java
@@ -690,7 +690,7 @@ public class JQOM2LuceneQueryBuilder implements QOMTreeVisitor, QueryObjectModel
                 NodeType[] superTypes = nt.getSupertypes();
                 if (Arrays.asList(superTypes).contains(base)) {
                     Name n = session.getQName(nt.getName());
                    String ntName = nsMappings.translatePropertyName(n);
                    String ntName = nsMappings.translateName(n);
                     Term t;
                     if (nt.isMixin()) {
                         // search on jcr:mixinTypes
@@ -740,7 +740,7 @@ public class JQOM2LuceneQueryBuilder implements QOMTreeVisitor, QueryObjectModel
                 return LongField.longToString(value.getLong());
             case PropertyType.NAME:
                 Name n = session.getQName(value.getString());
                return nsMappings.translatePropertyName(n);
                return nsMappings.translateName(n);
             case PropertyType.PATH:
                 Path p = session.getQPath(value.getString());
                 return npResolver.getJCRPath(p);
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryBuilder.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryBuilder.java
index 248e7a4bf..1b8cad6b4 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryBuilder.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryBuilder.java
@@ -330,7 +330,7 @@ public class LuceneQueryBuilder implements QueryNodeVisitor {
                 NodeType[] superTypes = nt.getSupertypes();
                 if (Arrays.asList(superTypes).contains(base)) {
                     Name n = session.getQName(nt.getName());
                    String ntName = nsMappings.translatePropertyName(n);
                    String ntName = nsMappings.translateName(n);
                     Term t;
                     if (nt.isMixin()) {
                         // search on jcr:mixinTypes
@@ -954,7 +954,7 @@ public class LuceneQueryBuilder implements QueryNodeVisitor {
                     // try to translate name
                     try {
                         Name n = session.getQName(literal);
                        values.add(nsMappings.translatePropertyName(n));
                        values.add(nsMappings.translateName(n));
                         log.debug("Coerced " + literal + " into NAME.");
                     } catch (NameException e) {
                         log.debug("Unable to coerce '" + literal + "' into a NAME: " + e.toString());
@@ -1028,7 +1028,7 @@ public class LuceneQueryBuilder implements QueryNodeVisitor {
                 // might be a name
                 try {
                     Name n = session.getQName(literal);
                    values.add(nsMappings.translatePropertyName(n));
                    values.add(nsMappings.translateName(n));
                     log.debug("Coerced " + literal + " into NAME.");
                 } catch (Exception e) {
                     // not a name
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NSRegistryBasedNamespaceMappings.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NSRegistryBasedNamespaceMappings.java
index f597e290c..3f50bdcff 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NSRegistryBasedNamespaceMappings.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NSRegistryBasedNamespaceMappings.java
@@ -16,11 +16,7 @@
  */
 package org.apache.jackrabbit.core.query.lucene;
 
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
 import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.Name;
 
 import javax.jcr.NamespaceException;
 
@@ -28,19 +24,13 @@ import javax.jcr.NamespaceException;
  * <code>NSRegistryBasedNamespaceMappings</code> implements a namespace mapping
  * based on the stable index prefix provided by the namespace registry.
  */
public class NSRegistryBasedNamespaceMappings
        implements NamespaceResolver, NamespaceMappings {
public class NSRegistryBasedNamespaceMappings extends AbstractNamespaceMappings {
 
     /**
      * The namespace registry.
      */
     private final NamespaceRegistryImpl nsReg;
 
        /**
     * The name resolver used to translate the qualified name to JCR name
     */
    private final NameResolver nameResolver;

     /**
      * Creates a new <code>NSRegistryBasedNamespaceMappings</code>.
      *
@@ -48,7 +38,6 @@ public class NSRegistryBasedNamespaceMappings
      */
     NSRegistryBasedNamespaceMappings(NamespaceRegistryImpl nsReg) {
         this.nsReg = nsReg;
        this.nameResolver = NamePathResolverImpl.create(this);
     }
 
     //-------------------------------< NamespaceResolver >----------------------
@@ -77,20 +66,4 @@ public class NSRegistryBasedNamespaceMappings
                     "Unknown namespace URI: " + uri, e);
         }
     }

    //-------------------------------< NamespaceMappings >----------------------

    /**
     * {@inheritDoc}
     */
    public String translatePropertyName(Name qName)
            throws IllegalNameException {
        try {
            return nameResolver.getJCRName(qName);
        } catch (NamespaceException e) {
            // should never happen actually, there is always a stable index
            // prefix for a known namespace uri
            throw new IllegalNameException("Internal error.", e);
        }
    }
 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NameQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NameQuery.java
index 6617dc708..9e729a72b 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NameQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NameQuery.java
@@ -87,7 +87,7 @@ public class NameQuery extends Query {
             // use LABEL field
             try {
                 return new TermQuery(new Term(FieldNames.LABEL,
                        nsMappings.translatePropertyName(nodeName)));
                        nsMappings.translateName(nodeName)));
             } catch (IllegalNameException e) {
                 throw Util.createIOException(e);
             }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NameRangeQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NameRangeQuery.java
index cf37fc2b4..3b68e4695 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NameRangeQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NameRangeQuery.java
@@ -164,7 +164,7 @@ public class NameRangeQuery extends Query {
             if (lowerName == null) {
                 text = nsMappings.getPrefix(upperName.getNamespaceURI()) + ":";
             } else {
                text = nsMappings.translatePropertyName(lowerName);
                text = nsMappings.translateName(lowerName);
             }
             return new Term(FieldNames.LABEL, text);
         } catch (RepositoryException e) {
@@ -182,7 +182,7 @@ public class NameRangeQuery extends Query {
             if (upperName == null) {
                 text = nsMappings.getPrefix(lowerName.getNamespaceURI()) + ":\uFFFF";
             } else {
                text = nsMappings.translatePropertyName(upperName);
                text = nsMappings.translateName(upperName);
             }
             return new Term(FieldNames.LABEL, text);
         } catch (RepositoryException e) {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NamespaceMappings.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NamespaceMappings.java
index f4793bb72..93cc6a42a 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NamespaceMappings.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NamespaceMappings.java
@@ -19,6 +19,7 @@ package org.apache.jackrabbit.core.query.lucene;
 import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
 import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
 import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
 
 /**
  * The class <code>NamespaceMappings</code> holds a namespace mapping that is
@@ -28,12 +29,21 @@ import org.apache.jackrabbit.spi.Name;
 public interface NamespaceMappings extends NamespaceResolver {
 
     /**
     * Translates a property name from a session local namespace mapping into a
     * search index private namespace mapping.
     * Translates a name from a session local namespace mapping into a search
     * index private namespace mapping.
      *
     * @param qName     the property name to translate
     * @return the translated JCR property name
     * @param name the name to translate
     * @return the translated JCR name
     * @throws IllegalNameException if the name cannot be translated.
      */
    String translatePropertyName(Name qName) throws IllegalNameException;
    String translateName(Name name) throws IllegalNameException;
 
    /**
     * Translates a path into a search index private namespace mapping.
     *
     * @param path the path to translate
     * @return the translated path.
     * @throws IllegalNameException if the name cannot be translated.
     */
    String translatePath(Path path) throws IllegalNameException;
 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java
index cf8700616..99a9d3f2f 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java
@@ -20,6 +20,7 @@ import org.apache.jackrabbit.core.ItemManager;
 import org.apache.jackrabbit.core.SessionImpl;
 import org.apache.jackrabbit.core.NodeId;
 import org.apache.jackrabbit.core.NodeIdIterator;
import org.apache.jackrabbit.core.HierarchyManager;
 import org.apache.jackrabbit.core.fs.FileSystem;
 import org.apache.jackrabbit.core.fs.FileSystemResource;
 import org.apache.jackrabbit.core.fs.FileSystemException;
@@ -33,6 +34,8 @@ import org.apache.jackrabbit.core.query.lucene.directory.FSDirectoryManager;
 import org.apache.jackrabbit.core.state.NodeState;
 import org.apache.jackrabbit.core.state.NodeStateIterator;
 import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.ItemStateException;
 import org.apache.jackrabbit.extractor.DefaultTextExtractor;
 import org.apache.jackrabbit.extractor.TextExtractor;
 import org.apache.jackrabbit.spi.Name;
@@ -46,6 +49,7 @@ import org.apache.jackrabbit.uuid.UUID;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.MultiReader;
 import org.apache.lucene.index.Term;
@@ -162,6 +166,11 @@ public class SearchIndex extends AbstractQueryHandler {
      */
     public static final int DEFAULT_TERM_INFOS_INDEX_DIVISOR = 1;
 
    /**
     * The path factory.
     */
    protected static final PathFactory PATH_FACTORY = PathFactoryImpl.getInstance();

     /**
      * The path of the root node.
      */
@@ -173,10 +182,9 @@ public class SearchIndex extends AbstractQueryHandler {
     private static final Path JCR_SYSTEM_PATH;
 
     static {
        PathFactory factory = PathFactoryImpl.getInstance();
        ROOT_PATH = factory.create(NameConstants.ROOT);
        ROOT_PATH = PATH_FACTORY.create(NameConstants.ROOT);
         try {
            JCR_SYSTEM_PATH = factory.create(ROOT_PATH, NameConstants.JCR_SYSTEM, false);
            JCR_SYSTEM_PATH = PATH_FACTORY.create(ROOT_PATH, NameConstants.JCR_SYSTEM, false);
         } catch (RepositoryException e) {
             // should never happen, path is always valid
             throw new InternalError(e.getMessage());
@@ -1168,29 +1176,69 @@ public class SearchIndex extends AbstractQueryHandler {
                 return;
             }
             try {
                ItemStateManager ism = getContext().getItemStateManager();
                 for (int i = 0; i < aggregateRules.length; i++) {
                    boolean ruleMatched = false;
                    // node includes
                     NodeState[] aggregates = aggregateRules[i].getAggregatedNodeStates(state);
                    if (aggregates == null) {
                        continue;
                    if (aggregates != null) {
                        ruleMatched = true;
                        for (int j = 0; j < aggregates.length; j++) {
                            Document aDoc = createDocument(aggregates[j],
                                    getNamespaceMappings(),
                                    index.getIndexFormatVersion());
                            // transfer fields to doc if there are any
                            Fieldable[] fulltextFields = aDoc.getFieldables(FieldNames.FULLTEXT);
                            if (fulltextFields != null) {
                                for (int k = 0; k < fulltextFields.length; k++) {
                                    doc.add(fulltextFields[k]);
                                }
                                doc.add(new Field(FieldNames.AGGREGATED_NODE_UUID,
                                        aggregates[j].getNodeId().getUUID().toString(),
                                        Field.Store.NO,
                                        Field.Index.NO_NORMS));
                            }
                        }
                     }
                    for (int j = 0; j < aggregates.length; j++) {
                        Document aDoc = createDocument(aggregates[j],
                                getNamespaceMappings(),
                                index.getIndexFormatVersion());
                        // transfer fields to doc if there are any
                        Fieldable[] fulltextFields = aDoc.getFieldables(FieldNames.FULLTEXT);
                        if (fulltextFields != null) {
                            for (int k = 0; k < fulltextFields.length; k++) {
                                doc.add(fulltextFields[k]);
                    // property includes
                    PropertyState[] propStates = aggregateRules[i].getAggregatedPropertyStates(state);
                    if (propStates != null) {
                        ruleMatched = true;
                        for (int j = 0; j < propStates.length; j++) {
                            PropertyState propState = propStates[j];
                            String namePrefix = FieldNames.createNamedValue(
                                    getNamespaceMappings().translateName(propState.getName()), "");
                            NodeState parent = (NodeState) ism.getItemState(propState.getParentId());
                            Document aDoc = createDocument(parent, getNamespaceMappings(), getIndex().getIndexFormatVersion());
                            // find the right fields to transfer
                            Fieldable[] fields = aDoc.getFieldables(FieldNames.PROPERTIES);
                            for (int k = 0; k < fields.length; k++) {
                                Fieldable field = fields[k];
                                // assume properties fields use SingleTokenStream
                                Token t = field.tokenStreamValue().next();
                                String value = new String(t.termBuffer(), 0, t.termLength());
                                if (value.startsWith(namePrefix)) {
                                    // extract value
                                    value = value.substring(namePrefix.length());
                                    // create new named value
                                    Path p = getRelativePath(state, propState);
                                    String path = getNamespaceMappings().translatePath(p);
                                    value = FieldNames.createNamedValue(path, value);
                                    t.setTermText(value);
                                    doc.add(new Field(field.name(), new SingletonTokenStream(t)));
                                    doc.add(new Field(FieldNames.AGGREGATED_NODE_UUID,
                                            parent.getNodeId().getUUID().toString(),
                                            Field.Store.NO,
                                            Field.Index.NO_NORMS));
                                }
                             }
                            doc.add(new Field(FieldNames.AGGREGATED_NODE_UUID,
                                    aggregates[j].getNodeId().getUUID().toString(),
                                    Field.Store.NO,
                                    Field.Index.NO_NORMS));
                         }
                     }

                     // only use first aggregate definition that matches
                    break;
                    if (ruleMatched) {
                        break;
                    }
                 }
             } catch (Exception e) {
                 // do not fail if aggregate cannot be created
@@ -1200,6 +1248,38 @@ public class SearchIndex extends AbstractQueryHandler {
         }
     }
 
    /**
     * Returns the relative path from <code>nodeState</code> to
     * <code>propState</code>.
     *
     * @param nodeState a node state.
     * @param propState a property state.
     * @return the relative path.
     * @throws RepositoryException if an error occurs while resolving paths.
     * @throws ItemStateException  if an error occurs while reading item
     *                             states.
     */
    protected Path getRelativePath(NodeState nodeState, PropertyState propState)
            throws RepositoryException, ItemStateException {
        HierarchyManager hmgr = getContext().getHierarchyManager();
        Path nodePath = hmgr.getPath(nodeState.getId());
        Path propPath = hmgr.getPath(propState.getId());
        Path p = nodePath.computeRelativePath(propPath);
        // make sure it does not contain indexes
        boolean clean = true;
        Path.Element[] elements = p.getElements();
        for (int i = 0; i < elements.length; i++) {
            if (elements[i].getIndex() != 0) {
                elements[i] = PATH_FACTORY.createElement(elements[i].getName());
                clean = false;
            }
        }
        if (!clean) {
            p = PATH_FACTORY.create(elements);
        }
        return p;
    }

     /**
      * Retrieves the root of the indexing aggregate for <code>state</code> and
      * puts it into <code>map</code>.
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java
index 533a39499..2dcaad6df 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldCache.java
@@ -18,8 +18,9 @@ package org.apache.jackrabbit.core.query.lucene;
 
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
 import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.index.TermDocs;
 import org.apache.lucene.search.SortComparator;
 
 import java.io.IOException;
@@ -27,6 +28,8 @@ import java.util.HashMap;
 import java.util.Map;
 import java.util.WeakHashMap;
 
import javax.jcr.PropertyType;

 /**
  * Implements a variant of the lucene class <code>org.apache.lucene.search.FieldCacheImpl</code>.
  * The lucene FieldCache class has some sort of support for custom comparators
@@ -38,7 +41,7 @@ class SharedFieldCache {
     /**
      * Expert: Stores term text values and document ordering data.
      */
    public static class StringIndex {
    public static class ValueIndex {
 
         /**
          * Some heuristic factor that determines whether the array is sparse. Note that if less then
@@ -48,62 +51,62 @@ class SharedFieldCache {
         private static final int SPARSE_FACTOR = 100;
 
         /**
         * Terms indexed by document id.
         * Values indexed by document id.
          */
        private final String[] terms;
        private final Comparable[] values;
 
         /**
         * Terms map indexed by document id.
         * Values (Comparable) map indexed by document id.
          */
        public final Map termsMap;
        public final Map valuesMap;
 
         /**
         * Boolean indicating whether the hashMap impl has to be used
         * Boolean indicating whether the {@link #valuesMap} impl has to be used
          */
         public final boolean sparse;
 
         /**
          * Creates one of these objects
          */
        public StringIndex(String[] terms, int setValues) {
            if (isSparse(terms, setValues)) {
        public ValueIndex(Comparable[] values, int setValues) {
            if (isSparse(values, setValues)) {
                 this.sparse = true;
                this.terms = null;
                this.values = null;
                 if (setValues == 0) {
                    this.termsMap = null;
                    this.valuesMap = null;
                 } else {
                    this.termsMap = getTermsMap(terms, setValues);
                    this.valuesMap = getValuesMap(values, setValues);
                 }
             } else {
                 this.sparse = false;
                this.terms = terms;
                this.termsMap = null;
                this.values = values;
                this.valuesMap = null;
             }
         }
 
        public String getTerm(int i) {
        public Comparable getValue(int i) {
             if (sparse) {
                return termsMap == null ? null : (String) termsMap.get(new Integer(i));
                return valuesMap == null ? null : (Comparable) valuesMap.get(new Integer(i));
             } else {
                return terms[i];
                return values[i];
             }
         }
 
        private Map getTermsMap(String[] terms, int setValues) {
        private Map getValuesMap(Comparable[] values, int setValues) {
             Map map = new HashMap(setValues);
            for (int i = 0; i < terms.length && setValues > 0; i++) {
                if (terms[i] != null) {
                    map.put(new Integer(i), terms[i]);
            for (int i = 0; i < values.length && setValues > 0; i++) {
                if (values[i] != null) {
                    map.put(new Integer(i), values[i]);
                     setValues--;
                 }
             }
             return map;
         }
 
        private boolean isSparse(String[] terms, int setValues) {
        private boolean isSparse(Comparable[] values, int setValues) {
             // some really simple test to test whether the array is sparse. Currently, when less then 1% is set, the array is already sparse 
             // for this typical cache to avoid memory issues
            if (setValues * SPARSE_FACTOR < terms.length) {
            if (setValues * SPARSE_FACTOR < values.length) {
                 return true;
             }
             return false;
@@ -127,26 +130,24 @@ class SharedFieldCache {
     }
 
     /**
     * Creates a <code>StringIndex</code> for a <code>field</code> and a term
     * Creates a <code>ValueIndex</code> for a <code>field</code> and a term
      * <code>prefix</code>. The term prefix acts as the property name for the
      * shared <code>field</code>.
      * <p/>
      * This method is an adapted version of: <code>FieldCacheImpl.getStringIndex()</code>
     * The returned string index will <b>not</b> have a term lookup array!
     * See {@link SharedFieldSortComparator} for more info.
      *
      * @param reader     the <code>IndexReader</code>.
      * @param field      name of the shared field.
      * @param prefix     the property name, will be used as term prefix.
      * @param comparator the sort comparator instance.
     * @return a StringIndex that contains the field values and order
     * @return a ValueIndex that contains the field values and order
      *         information.
      * @throws IOException if an error occurs while reading from the index.
      */
    public SharedFieldCache.StringIndex getStringIndex(IndexReader reader,
                                                 String field,
                                                 String prefix,
                                                 SortComparator comparator)
    public ValueIndex getValueIndex(IndexReader reader,
                                    String field,
                                    String prefix,
                                    SortComparator comparator)
             throws IOException {
 
         if (reader instanceof ReadOnlyIndexReader) {
@@ -154,12 +155,22 @@ class SharedFieldCache {
         }
 
         field = field.intern();
        SharedFieldCache.StringIndex ret = lookup(reader, field, prefix, comparator);
        ValueIndex ret = lookup(reader, field, prefix, comparator);
         if (ret == null) {
            final String[] retArray = new String[reader.maxDoc()];
            Comparable[] retArray = new Comparable[reader.maxDoc()];
             int setValues = 0;
             if (retArray.length > 0) {
                TermDocs termDocs = reader.termDocs();
                IndexFormatVersion version = IndexFormatVersion.getVersion(reader);
                boolean hasPayloads = version.isAtLeast(IndexFormatVersion.V3);
                TermDocs termDocs;
                byte[] payload = null;
                int type;
                if (hasPayloads) {
                    termDocs = reader.termPositions();
                    payload = new byte[1];
                } else {
                    termDocs = reader.termDocs();
                }
                 TermEnum termEnum = reader.terms(new Term(field, prefix));
 
                 char[] tmp = new char[16];
@@ -185,8 +196,17 @@ class SharedFieldCache {
 
                         termDocs.seek(termEnum);
                         while (termDocs.next()) {
                            type = PropertyType.UNDEFINED;
                            if (hasPayloads) {
                                TermPositions termPos = (TermPositions) termDocs;
                                termPos.nextPosition();
                                if (termPos.isPayloadAvailable()) {
                                    payload = termPos.getPayload(payload, 0);
                                    type = PropertyMetaData.fromByteArray(payload).getPropertyType();
                                }
                            }
                             setValues++;
                            retArray[termDocs.doc()] = value;
                            retArray[termDocs.doc()] = getValue(value, type);
                         }
                     } while (termEnum.next());
                 } finally {
@@ -194,7 +214,7 @@ class SharedFieldCache {
                     termEnum.close();
                 }
             }
            SharedFieldCache.StringIndex value = new SharedFieldCache.StringIndex(retArray, setValues);
            ValueIndex value = new ValueIndex(retArray, setValues);
             store(reader, field, prefix, comparator, value);
             return value;
         }
@@ -202,9 +222,9 @@ class SharedFieldCache {
     }
 
     /**
     * See if a <code>StringIndex</code> object is in the cache.
     * See if a <code>ValueIndex</code> object is in the cache.
      */
    SharedFieldCache.StringIndex lookup(IndexReader reader, String field,
    ValueIndex lookup(IndexReader reader, String field,
                                   String prefix, SortComparator comparer) {
         Key key = new Key(field, prefix, comparer);
         synchronized (this) {
@@ -212,15 +232,15 @@ class SharedFieldCache {
             if (readerCache == null) {
                 return null;
             }
            return (SharedFieldCache.StringIndex) readerCache.get(key);
            return (ValueIndex) readerCache.get(key);
         }
     }
 
     /**
     * Put a <code>StringIndex</code> <code>value</code> to cache.
     * Put a <code>ValueIndex</code> <code>value</code> to cache.
      */
     Object store(IndexReader reader, String field, String prefix,
                 SortComparator comparer, SharedFieldCache.StringIndex value) {
                 SortComparator comparer, ValueIndex value) {
         Key key = new Key(field, prefix, comparer);
         synchronized (this) {
             HashMap readerCache = (HashMap) cache.get(reader);
@@ -232,6 +252,29 @@ class SharedFieldCache {
         }
     }
 
    /**
     * Returns a comparable for the given <code>value</code> that is read from
     * the index.
     *
     * @param value the value as read from the index.
     * @param type the property type.
     * @return a comparable for the <code>value</code>.
     */
    private Comparable getValue(String value, int type) {
        switch (type) {
            case PropertyType.BOOLEAN:
                return ComparableBoolean.valueOf(Boolean.valueOf(value).booleanValue());
            case PropertyType.DATE:
                return new Long(DateField.stringToTime(value));
            case PropertyType.LONG:
                return new Long(LongField.stringToLong(value));
            case PropertyType.DOUBLE:
                return new Double(DoubleField.stringToDouble(value));
            default:
                return value;
        }
    }

     /**
      * A compound <code>Key</code> that consist of <code>field</code>
      * <code>prefix</code> and <code>comparator</code>.
@@ -243,7 +286,7 @@ class SharedFieldCache {
         private final SortComparator comparator;
 
         /**
         * Creates <code>Key</code> for StringIndex lookup.
         * Creates <code>Key</code> for ValueIndex lookup.
          */
         Key(String field, String prefix, SortComparator comparator) {
             this.field = field.intern();
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldSortComparator.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldSortComparator.java
index 1a06f70e4..fd8ee0dcb 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldSortComparator.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SharedFieldSortComparator.java
@@ -44,13 +44,6 @@ import org.apache.jackrabbit.uuid.UUID;
 /**
  * Implements a <code>SortComparator</code> which knows how to sort on a lucene
  * field that contains values for multiple properties.
 * <p/>
 * <b>Important:</b> The ScoreDocComparator returned by {@link #newComparator}
 * does not implement the contract for {@link ScoreDocComparator#sortValue(ScoreDoc)}
 * properly. The method will always return an empty String to save memory consumption
 * on large property ranges. Those values are only of relevance when queries
 * are executed with a <code>MultiSearcher</code>, which is currently not the
 * case in Jackrabbit.
  */
 public class SharedFieldSortComparator extends SortComparator {
 
@@ -108,15 +101,20 @@ public class SharedFieldSortComparator extends SortComparator {
             throws IOException {
         PathFactory factory = PathFactoryImpl.getInstance();
         Path p = factory.create(relPath);
        if (p.getLength() == 1) {
            try {
                return new SimpleScoreDocComparator(reader,
                        nsMappings.translatePropertyName(p.getNameElement().getName()));
            } catch (IllegalNameException e) {
                throw Util.createIOException(e);
        try {
            ScoreDocComparator simple = new SimpleScoreDocComparator(
                    reader, nsMappings.translatePath(p));
            if (p.getLength() == 1) {
                return simple;
            } else {
                return new CompoundScoreDocComparator(reader,
                        new ScoreDocComparator[]{
                                simple,
                                new RelPathScoreDocComparator(reader, p)
                        });
             }
        } else {
            return new RelPathScoreDocComparator(reader, p);
        } catch (IllegalNameException e) {
            throw Util.createIOException(e);
         }
     }
 
@@ -250,17 +248,17 @@ public class SharedFieldSortComparator extends SortComparator {
         /**
          * The term look ups of the index segments.
          */
        protected final SharedFieldCache.StringIndex[] indexes;
        protected final SharedFieldCache.ValueIndex[] indexes;
 
         public SimpleScoreDocComparator(IndexReader reader,
                                         String propertyName)
                 throws IOException {
             super(reader);
            this.indexes = new SharedFieldCache.StringIndex[readers.size()];
            this.indexes = new SharedFieldCache.ValueIndex[readers.size()];
 
             for (int i = 0; i < readers.size(); i++) {
                 IndexReader r = (IndexReader) readers.get(i);
                indexes[i] = SharedFieldCache.INSTANCE.getStringIndex(r, field,
                indexes[i] = SharedFieldCache.INSTANCE.getValueIndex(r, field,
                         FieldNames.createNamedValue(propertyName, ""),
                         SharedFieldSortComparator.this);
             }
@@ -274,7 +272,7 @@ public class SharedFieldSortComparator extends SortComparator {
          */
         public Comparable sortValue(ScoreDoc i) {
             int idx = readerIndex(i.doc);
            return indexes[idx].getTerm(i.doc - starts[idx]);
            return indexes[idx].getValue(i.doc - starts[idx]);
         }
     }
 
@@ -359,28 +357,34 @@ public class SharedFieldSortComparator extends SortComparator {
     }
 
     /**
     * Represents a boolean that implement {@link Comparable}. This class can
     * be removed when we move to Java 5.
     * Implements a compound score doc comparator that delegates to several
     * other comparators. The comparators are asked for a sort value in the
     * sequence they are passed to the constructor. The first non-null value
     * will be returned by {@link #sortValue(ScoreDoc)}.
      */
    private static final class ComparableBoolean implements Comparable {

        private static final ComparableBoolean TRUE = new ComparableBoolean(true);
    private final class CompoundScoreDocComparator
            extends AbstractScoreDocComparator {
 
        private static final ComparableBoolean FALSE = new ComparableBoolean(false);

        private final boolean value;

        private ComparableBoolean(boolean value) {
            this.value = value;
        }
        private final ScoreDocComparator[] comparators;
 
        public int compareTo(Object o) {
            ComparableBoolean b = (ComparableBoolean) o;
            return (b.value == value ? 0 : (value ? 1 : -1));
        public CompoundScoreDocComparator(IndexReader reader,
                                          ScoreDocComparator[] comparators)
                throws IOException {
            super(reader);
            this.comparators = comparators;
         }
 
        static ComparableBoolean valueOf(boolean value) {
            return value ? TRUE : FALSE;
        /**
         * {@inheritDoc}
         */
        public Comparable sortValue(ScoreDoc i) {
            for (int j = 0; j < comparators.length; j++) {
                Comparable c = comparators[j].sortValue(i);
                if (c != null) {
                    return c;
                }
            }
            return null;
         }
     }
 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SingletonTokenStream.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SingletonTokenStream.java
index 7d581a781..1e9fa40e5 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SingletonTokenStream.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SingletonTokenStream.java
@@ -45,6 +45,15 @@ public final class SingletonTokenStream extends TokenStream {
         t.setPayload(new Payload(new PropertyMetaData(type).toByteArray()));
     }
 
    /**
     * Creates a new SingleTokenStream with the given token.
     *
     * @param t the token.
     */
    public SingletonTokenStream(Token t) {
        this.t = t;
    }

     /**
      * {@inheritDoc}
      */
diff --git a/jackrabbit-core/src/main/resources/org/apache/jackrabbit/core/query/lucene/indexing-configuration-1.2.dtd b/jackrabbit-core/src/main/resources/org/apache/jackrabbit/core/query/lucene/indexing-configuration-1.2.dtd
new file mode 100644
index 000000000..ad5bdda16
-- /dev/null
++ b/jackrabbit-core/src/main/resources/org/apache/jackrabbit/core/query/lucene/indexing-configuration-1.2.dtd
@@ -0,0 +1,93 @@
<!--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->
<!--
    The configuration element configures the indexing behaviour of the lucene
    backed query handler in Jackrabbit. It allows you to define indexing
    aggregates and configure which properties of a node are indexed.
    This element must contain all the namespace declarations that are used
    throughout this configuration.
-->
<!ELEMENT configuration (aggregate*,index-rule*)>

<!--
    Each aggregate element defines an indexing aggregate based on the name of a
    primary node type.
-->
<!ELEMENT aggregate (include*,include-property)>
<!ATTLIST aggregate primaryType CDATA #REQUIRED>

<!--
    An include element contains a relative path pattern using either an exact
    node name or *. Nodes that match the path pattern against the root of an
    indexing aggregate are included in the aggregated node index. An include
    element may optionally specify a primary node type name that needs to match
    for the included node.
-->
<!ELEMENT include (#PCDATA)>
<!ATTLIST include primaryType CDATA #IMPLIED>

<!--
    An include-property element contains a relative path to a property. Properties
    that match the path against the root of an indexing aggregate are included
    in the aggregated node index. Aggregated properties may be used to speed
    up sorting of query results when the order by clause references a property
    with a relative path.
-->
<!ELEMENT include-property (#PCDATA)>

<!--
    An index-rule element defines which properties of a node should be indexed.
    When a node is indexed the list of index-rules is check for a matching
    node type and whether the condition is true. If a match is found the
    property is looked up.
    The index-rule element also contains a boost value for the entire node
    being indexed. A value higher than 1.0 will boost the score value for a node
    that matched this index-rule.
-->
<!ELEMENT index-rule (property*)>
<!ATTLIST index-rule nodeType CDATA #REQUIRED
                     condition CDATA #IMPLIED
                     boost CDATA "1.0">

<!--
    A property element defines the boost value for a matching property and a
    flag that indicates whether the value of a string property should also be
    included in the node scope fulltext index. Both boost and nodeScopeIndex
    attributes only affect string properties and are ignored if the property
    is not of type string. If isRegexp is set to true the name of the property
    is interpreted as a regular expression to match properties on a node. Please
    note that you may only use a regular expression for the local part of a
    property name. The attribute useInExcerpt controls whether the contents
    of the property is used to construct an excerpt. The default value for this
    attribute is true.
-->
<!ELEMENT property (#PCDATA)>
<!ATTLIST property boost CDATA "1.0"
                   nodeScopeIndex CDATA "true"
                   isRegexp CDATA "false"
                   useInExcerpt CDATA "true">

<!--
    An analyzer element with property elements in it defines which analyzer is to
    be used for indexing and parsing the full text of this property. If the analyzer
    class can not be found, the default analyzer is used. The node scope is always
    indexed with the default analyzer, so might return different results for search
    queries in some rare cases.
-->
<!ELEMENT analyzers (analyzer*)>
<!ELEMENT analyzer (property*)>
<!ATTLIST analyzer class CDATA #REQUIRED>
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/IndexingAggregateTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/IndexingAggregateTest.java
index 2e34e4f6e..c16396614 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/IndexingAggregateTest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/IndexingAggregateTest.java
@@ -18,12 +18,18 @@ package org.apache.jackrabbit.core.query;
 
 import javax.jcr.RepositoryException;
 import javax.jcr.Node;
import javax.jcr.query.Query;

 import java.io.ByteArrayOutputStream;
 import java.io.Writer;
 import java.io.OutputStreamWriter;
 import java.io.ByteArrayInputStream;
 import java.io.IOException;
 import java.util.Calendar;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
 
 /**
  * <code>IndexingAggregateTest</code> checks if the nt:file nt:resource
@@ -89,4 +95,96 @@ public class IndexingAggregateTest extends AbstractIndexingTest {
 
         executeSQLQuery(sqlCat, new Node[]{file});
     }

    public void testContentLastModified() throws RepositoryException {
        List expected = new ArrayList();
        long time = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            expected.add(addFile(testRootNode, "file" + i, time));
            time += 1000;
        }
        testRootNode.save();

        String stmt = testPath + "/* order by jcr:content/@jcr:lastModified";
        Query q = qm.createQuery(stmt, Query.XPATH);
        checkResultSequence(q.execute().getRows(), (Node[]) expected.toArray(new Node[expected.size()]));

        // descending
        stmt = testPath + "/* order by jcr:content/@jcr:lastModified descending";
        q = qm.createQuery(stmt, Query.XPATH);
        Collections.reverse(expected);
        checkResultSequence(q.execute().getRows(), (Node[]) expected.toArray(new Node[expected.size()]));

        // reverse order in content
        for (Iterator it = expected.iterator(); it.hasNext(); ) {
            Node file = (Node) it.next();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(time);
            file.getNode("jcr:content").setProperty("jcr:lastModified", cal);
            time -= 1000;
        }
        testRootNode.save();

        stmt = testPath + "/* order by jcr:content/@jcr:lastModified descending";
        q = qm.createQuery(stmt, Query.XPATH);
        checkResultSequence(q.execute().getRows(), (Node[]) expected.toArray(new Node[expected.size()]));
    }

    public void disabled_testPerformance() throws RepositoryException {
        createNodes(testRootNode, 10, 4, 0, new NodeCreationCallback() {
            public void nodeCreated(Node node, int count) throws
                    RepositoryException {
                node.addNode("child").setProperty("property", "value" + count);
                // save once in a while
                if (count % 1000 == 0) {
                    session.save();
                    System.out.println("added " + count + " nodes so far.");
                }
            }
        });
        session.save();

        String xpath = testPath + "//*[child/@property] order by child/@property";
        for (int i = 0; i < 3; i++) {
            long time = System.currentTimeMillis();
            Query query = qm.createQuery(xpath, Query.XPATH);
            ((QueryImpl) query).setLimit(20);
            query.execute().getNodes().getSize();
            time = System.currentTimeMillis() - time;
            System.out.println("executed query in " + time + " ms.");
        }
    }

    private static Node addFile(Node folder, String name, long lastModified)
            throws RepositoryException {
        Node file = folder.addNode(name, "nt:file");
        Node resource = file.addNode("jcr:content", "nt:resource");
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(lastModified);
        resource.setProperty("jcr:lastModified", cal);
        resource.setProperty("jcr:encoding", "UTF-8");
        resource.setProperty("jcr:mimeType", "text/plain");
        resource.setProperty("jcr:data", new ByteArrayInputStream("test".getBytes()));
        return file;
    }

    private int createNodes(Node n, int nodesPerLevel, int levels,
                            int count, NodeCreationCallback callback)
            throws RepositoryException {
        levels--;
        for (int i = 0; i < nodesPerLevel; i++) {
            Node child = n.addNode("node" + i);
            count++;
            callback.nodeCreated(child, count);
            if (levels > 0) {
                count = createNodes(child, nodesPerLevel, levels, count, callback);
            }
        }
        return count;
    }

    private static interface NodeCreationCallback {

        public void nodeCreated(Node node, int count) throws RepositoryException;
    }
 }
diff --git a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/OrderByTest.java b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/OrderByTest.java
index 419c50bc6..673071c40 100644
-- a/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/OrderByTest.java
++ b/jackrabbit-core/src/test/java/org/apache/jackrabbit/core/query/OrderByTest.java
@@ -145,31 +145,6 @@ public class OrderByTest extends AbstractQueryTest {
         checkChildAxis(new Value[]{getValue(2.0), getValue(1)});
     }
 
    public void disabled_testPerformance() throws RepositoryException {
        createNodes(testRootNode, 10, 4, 0, new NodeCreationCallback() {
            public void nodeCreated(Node node, int count) throws
                    RepositoryException {
                node.addNode("child").setProperty("property", "value" + count);
                // save once in a while
                if (count % 1000 == 0) {
                    superuser.save();
                    System.out.println("added " + count + " nodes so far.");
                }
            }
        });
        superuser.save();

        String xpath = testPath + "//*[child/@property] order by child/@property";
        for (int i = 0; i < 3; i++) {
            long time = System.currentTimeMillis();
            Query query = qm.createQuery(xpath, Query.XPATH);
            ((QueryImpl) query).setLimit(20);
            query.execute().getNodes().getSize();
            time = System.currentTimeMillis() - time;
            System.out.println("executed query in " + time + " ms.");
        }
    }

     //------------------------------< helper >----------------------------------
 
     private Value getValue(String value) throws RepositoryException {
@@ -208,27 +183,55 @@ public class OrderByTest extends AbstractQueryTest {
      * @throws RepositoryException if an error occurs.
      */
     private void checkChildAxis(Value[] values) throws RepositoryException {
        // child/prop is part of the test indexing configuration,
        // this will use SimpleScoreDocComparator internally
        checkChildAxis(values, "child", "prop");
        cleanUpTestRoot(superuser);
        // c/p is not in the indexing configuration,
        // this will use RelPathScoreDocComparator internally
        checkChildAxis(values, "c", "p");
    }

    /**
     * Checks if order by with a relative path works on the the passed values.
     * The values are expected to be in ascending order.
     *
     * @param values   the values in ascending order.
     * @param child    the name of the child node.
     * @param property the name of the property.
     * @throws RepositoryException if an error occurs.
     */
    private void checkChildAxis(Value[] values, String child, String property)
            throws RepositoryException {
        List vals = new ArrayList();
        // add initial value null -> property not set
        // inexistent property is always less than any property value set
        vals.add(null);
        vals.addAll(Arrays.asList(values));

         List expected = new ArrayList();
        for (int i = 0; i < values.length; i++) {
        for (int i = 0; i < vals.size(); i++) {
             Node n = testRootNode.addNode("node" + i);
             expected.add(n.getPath());
            n.addNode("child").setProperty("prop", values[i]);
            Node c = n.addNode(child);
            if (vals.get(i) != null) {
                c.setProperty(property, (Value) vals.get(i));
            }
         }
         testRootNode.save();
 
        String xpath = testPath + "/* order by child/@prop";
        String xpath = testPath + "/* order by " + child + "/@" + property;
         assertEquals(expected, collectPaths(executeQuery(xpath)));
 
         // descending
         Collections.reverse(expected);
        xpath = testPath + "/* order by child/@prop descending";
        xpath += " descending";
         assertEquals(expected, collectPaths(executeQuery(xpath)));
 
        // reverse order in content
        Collections.reverse(Arrays.asList(values));
        for (int i = 0; i < values.length; i++) {
            Node child = testRootNode.getNode("node" + i).getNode("child");
            child.setProperty("prop", values[i]);
        Collections.reverse(vals);
        for (int i = 0; i < vals.size(); i++) {
            Node c = testRootNode.getNode("node" + i).getNode(child);
            c.setProperty(property, (Value) vals.get(i));
         }
         testRootNode.save();
 
@@ -244,24 +247,4 @@ public class OrderByTest extends AbstractQueryTest {
         }
         return paths;
     }

    private int createNodes(Node n, int nodesPerLevel, int levels,
                            int count, NodeCreationCallback callback)
            throws RepositoryException {
        levels--;
        for (int i = 0; i < nodesPerLevel; i++) {
            Node child = n.addNode("node" + i);
            count++;
            callback.nodeCreated(child, count);
            if (levels > 0) {
                count = createNodes(child, nodesPerLevel, levels, count, callback);
            }
        }
        return count;
    }

    private static interface NodeCreationCallback {

        public void nodeCreated(Node node, int count) throws RepositoryException;
    }
 }
diff --git a/jackrabbit-core/src/test/repository/workspaces/default/indexing-configuration.xml b/jackrabbit-core/src/test/repository/workspaces/default/indexing-configuration.xml
new file mode 100644
index 000000000..5d83543d8
-- /dev/null
++ b/jackrabbit-core/src/test/repository/workspaces/default/indexing-configuration.xml
@@ -0,0 +1,10 @@
<?xml version="1.0"?>
<!DOCTYPE configuration SYSTEM "http://jackrabbit.apache.org/dtd/indexing-configuration-1.2.dtd">
<configuration xmlns:jcr="http://www.jcp.org/jcr/1.0"
               xmlns:nt="http://www.jcp.org/jcr/nt/1.0">

    <aggregate primaryType="nt:unstructured">
        <include-property>child/prop</include-property>
    </aggregate>

</configuration>
\ No newline at end of file
diff --git a/jackrabbit-core/src/test/repository/workspaces/default/workspace.xml b/jackrabbit-core/src/test/repository/workspaces/default/workspace.xml
index a0baa74f9..35ecb432a 100644
-- a/jackrabbit-core/src/test/repository/workspaces/default/workspace.xml
++ b/jackrabbit-core/src/test/repository/workspaces/default/workspace.xml
@@ -41,6 +41,7 @@
     <param name="synonymProviderConfigPath" value="../synonyms.properties"/>
     <param name="supportHighlighting" value="true"/>
     <param name="excerptProviderClass" value="org.apache.jackrabbit.core.query.lucene.WeightedHTMLExcerpt"/>
    <param name="indexingConfiguration" value="${wsp.home}/indexing-configuration.xml"/>
   </SearchIndex>
 </Workspace>
 
diff --git a/jackrabbit-core/src/test/repository/workspaces/indexing-test/indexing-configuration.xml b/jackrabbit-core/src/test/repository/workspaces/indexing-test/indexing-configuration.xml
index 74c799c0d..1fe1da536 100644
-- a/jackrabbit-core/src/test/repository/workspaces/indexing-test/indexing-configuration.xml
++ b/jackrabbit-core/src/test/repository/workspaces/indexing-test/indexing-configuration.xml
@@ -1,5 +1,5 @@
 <?xml version="1.0"?>
<!DOCTYPE configuration SYSTEM "http://jackrabbit.apache.org/dtd/indexing-configuration-1.1.dtd">
<!DOCTYPE configuration SYSTEM "http://jackrabbit.apache.org/dtd/indexing-configuration-1.2.dtd">
 <configuration xmlns:jcr="http://www.jcp.org/jcr/1.0"
                xmlns:nt="http://www.jcp.org/jcr/nt/1.0">
 
@@ -44,6 +44,11 @@
     <aggregate primaryType="nt:file">
         <include>jcr:content</include>
         <include>jcr:content/*</include>
        <include-property>jcr:content/jcr:lastModified</include-property>
    </aggregate>

    <aggregate primaryType="nt:unstructured">
        <include-property>child/property</include-property>
     </aggregate>
 
 </configuration>
\ No newline at end of file
- 
2.19.1.windows.1

