From 2fc7e19faa7cac6c46dce7d67cb95d1d902c7e1c Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Wed, 20 Feb 2008 13:34:50 +0000
Subject: [PATCH] JCR-1365: Query path constraints like foo//*/bar do not scale
 - more performance improvements

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@629453 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/query/lucene/AbstractQueryHits.java  |  19 +--
 .../core/query/lucene/ChildAxisQuery.java     |  37 +++++-
 .../query/lucene/ChildNodesQueryHits.java     | 110 ++++++++++++++++
 .../query/lucene/DescendantSelfAxisQuery.java | 124 +++++++++++++++++-
 .../core/query/lucene/FilterQueryHits.java    |  69 ++++++++++
 .../query/lucene/JackrabbitIndexSearcher.java |  24 ++--
 .../core/query/lucene/JackrabbitQuery.java    |  52 ++++++++
 .../core/query/lucene/LuceneQueryBuilder.java |   1 -
 .../core/query/lucene/LuceneQueryHits.java    |   7 -
 .../core/query/lucene/MatchAllDocsQuery.java  |  52 ++++++++
 .../query/lucene/NodeTraversingQueryHits.java |  85 +++++++-----
 .../core/query/lucene/SearchIndex.java        |  13 +-
 12 files changed, 515 insertions(+), 78 deletions(-)
 create mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ChildNodesQueryHits.java
 create mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FilterQueryHits.java
 create mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitQuery.java
 create mode 100644 jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllDocsQuery.java

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractQueryHits.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractQueryHits.java
index fc2bbc13c..bafd623b9 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractQueryHits.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/AbstractQueryHits.java
@@ -25,16 +25,9 @@ import java.io.IOException;
 public abstract class AbstractQueryHits implements QueryHits {
 
     /**
     * Calls {@link #doClose()} and disposes the {@link PerQueryCache}.
     *
     * @throws IOException if an error occurs while releasing resources.
     * This default implemetation does nothing.
      */
    public final void close() throws IOException {
        try {
            doClose();
        } finally {
            PerQueryCache.getInstance().dispose();
        }
    public void close() throws IOException {
     }
 
     /**
@@ -61,9 +54,11 @@ public abstract class AbstractQueryHits implements QueryHits {
     }
 
     /**
     * Releases resources held by this hits instance.
     * This default implementation returns <code>-1</code>.
      *
     * @throws IOException if an error occurs while releasing resources.
     * @return <code>-1</code>.
      */
    protected abstract void doClose() throws IOException;
    public int getSize() {
        return -1;
    }
 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ChildAxisQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ChildAxisQuery.java
index eae583d24..58b53d135 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ChildAxisQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ChildAxisQuery.java
@@ -17,6 +17,7 @@
 package org.apache.jackrabbit.core.query.lucene;
 
 import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.SessionImpl;
 import org.apache.jackrabbit.core.query.lucene.hits.AdaptingHits;
 import org.apache.jackrabbit.core.query.lucene.hits.Hits;
 import org.apache.jackrabbit.core.query.lucene.hits.ScorerHits;
@@ -38,7 +39,7 @@ import org.apache.lucene.search.Searcher;
 import org.apache.lucene.search.Similarity;
 import org.apache.lucene.search.TermQuery;
 import org.apache.lucene.search.Weight;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Sort;
 
 import java.io.IOException;
 import java.util.Iterator;
@@ -51,7 +52,7 @@ import java.util.Map;
  * Implements a lucene <code>Query</code> which returns the child nodes of the
  * nodes selected by another <code>Query</code>.
  */
class ChildAxisQuery extends Query {
class ChildAxisQuery extends Query implements JackrabbitQuery {
 
     /**
      * The item state manager containing persistent item states.
@@ -117,6 +118,21 @@ class ChildAxisQuery extends Query {
         this.position = position;
     }
 
    /**
     * @return the context query of this child axis query.
     */
    Query getContextQuery() {
        return contextQuery;
    }

    /**
     * @return <code>true</code> if this child axis query matches any child
     *         node; <code>false</code> otherwise.
     */
    boolean matchesAnyChildNode() {
        return nameTest == null && position == LocationStepQueryNode.NONE;
    }

     /**
      * @return the name test or <code>null</code> if none was specified.
      */
@@ -189,6 +205,23 @@ class ChildAxisQuery extends Query {
         return "ChildAxisQuery";
     }
 
    //-------------------< JackrabbitQuery >------------------------------------

    /**
     * {@inheritDoc}
     */
    public QueryHits execute(JackrabbitIndexSearcher searcher,
                             SessionImpl session,
                             Sort sort)
            throws IOException {
        if (sort.getSort().length == 0 && matchesAnyChildNode()) {
            Query context = getContextQuery();
            return new ChildNodesQueryHits(searcher.execute(context, sort), session);
        } else {
            return null;
        }
    }

     //-------------------< ChildAxisWeight >------------------------------------
 
     /**
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ChildNodesQueryHits.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ChildNodesQueryHits.java
new file mode 100644
index 000000000..6cf9217bb
-- /dev/null
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ChildNodesQueryHits.java
@@ -0,0 +1,110 @@
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

import org.apache.jackrabbit.core.SessionImpl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;

/**
 * <code>ChildNodesQueryHits</code> implements query hits that returns the child
 * nodes of another given query hits.
 */
public class ChildNodesQueryHits extends AbstractQueryHits {

    /**
     * The parent query hits.
     */
    private final QueryHits parents;

    /**
     * This session that executes the query.
     */
    private final SessionImpl session;

    /**
     * The current child hits.
     */
    private QueryHits childHits;

    /**
     * Creates a new <code>ChildNodesQueryHits</code> that returns the child
     * nodes of all query hits from the given <code>parents</code>.
     *
     * @param parents the parent query hits.
     * @param session the session that executes the query.
     * @throws IOException if an error occurs while reading from
     *                     <code>parents</code>
     */
    public ChildNodesQueryHits(QueryHits parents, SessionImpl session)
            throws IOException {
        this.parents = parents;
        this.session = session;
        fetchNextChildHits();
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        if (childHits != null) {
            childHits.close();
        }
        parents.close();
    }

    /**
     * {@inheritDoc}
     */
    public ScoreNode nextScoreNode() throws IOException {
        while (childHits != null) {
            ScoreNode sn = childHits.nextScoreNode();
            if (sn != null) {
                return sn;
            } else {
                fetchNextChildHits();
            }
        }
        // if we get here there are no more score nodes
        return null;
    }

    /**
     * Fetches the next {@link #childHits}
     * @throws IOException
     */
    private void fetchNextChildHits() throws IOException {
        if (childHits != null) {
            childHits.close();
        }
        ScoreNode nextParent = parents.nextScoreNode();
        if (nextParent != null) {
            try {
                Node parent = session.getNodeById(nextParent.getNodeId());
                childHits = new NodeTraversingQueryHits(parent, false, 1);
            } catch (RepositoryException e) {
                IOException ex = new IOException(e.getMessage());
                ex.initCause(e);
                throw ex;
            }
        } else {
            childHits = null;
        }
    }
}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DescendantSelfAxisQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DescendantSelfAxisQuery.java
index eac0155d9..6142416c3 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DescendantSelfAxisQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DescendantSelfAxisQuery.java
@@ -24,18 +24,24 @@ import org.apache.lucene.search.Scorer;
 import org.apache.lucene.search.Searcher;
 import org.apache.lucene.search.Similarity;
 import org.apache.lucene.search.Weight;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Sort;
import org.apache.jackrabbit.core.SessionImpl;
 
import javax.jcr.Node;
import javax.jcr.RepositoryException;
 import java.io.IOException;
 import java.util.BitSet;
 import java.util.Set;
import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;
 
 /**
  * Implements a lucene <code>Query</code> which filters a sub query by checking
  * whether the nodes selected by that sub query are descendants or self of
  * nodes selected by a context query.
  */
class DescendantSelfAxisQuery extends Query {
class DescendantSelfAxisQuery extends Query implements JackrabbitQuery {
 
     /**
      * The context query
@@ -134,6 +140,17 @@ class DescendantSelfAxisQuery extends Query {
     }
 
     /**
     * Returns the minimal levels required between context and sub nodes for a
     * sub node to match.
     * <ul>
     * <li><code>0</code>: a sub node <code>S</code> matches if it is a context
     * node or one of the ancestors of <code>S</code> is a context node.</li>
     * <li><code>1</code>: a sub node <code>S</code> matches if one of the
     * ancestors of <code>S</code> is a context node.</li>
     * <li><code>n</code>: a sub node <code>S</code> matches if
     * <code>S.getAncestor(S.getDepth() - n)</code> is a context node.</li>
     * </ul>
     *
      * @return the minimal levels required between context and sub nodes for a
      *         sub node to match.
      */
@@ -189,7 +206,108 @@ class DescendantSelfAxisQuery extends Query {
         }
     }
 
    //------------------------< DescendantSelfAxisWeight >--------------------------
    //------------------------< JackrabbitQuery >-------------------------------

    /**
     * {@inheritDoc}
     */
    public QueryHits execute(final JackrabbitIndexSearcher searcher,
                             final SessionImpl session,
                             final Sort sort) throws IOException {
        if (sort.getSort().length == 0 && subQueryMatchesAll()) {
            // maps path String to NodeId
            Map startingPoints = new TreeMap();
            QueryHits result = searcher.execute(getContextQuery(), sort);
            try {
                // minLevels 0 and 1 are handled with a series of
                // NodeTraversingQueryHits directly on result. For minLevels >= 2
                // intermediate ChildNodesQueryHits are required.
                for (int i = 2; i <= getMinLevels(); i++) {
                    result = new ChildNodesQueryHits(result, session);
                }

                ScoreNode sn;
                try {
                    while ((sn = result.nextScoreNode()) != null) {
                        Node node = session.getNodeById(sn.getNodeId());
                        startingPoints.put(node.getPath(), sn);
                    }
                } catch (RepositoryException e) {
                    IOException ex = new IOException(e.getMessage());
                    ex.initCause(e);
                    throw ex;
                }
            } finally {
                result.close();
            }

            // prune overlapping starting points
            String previousPath = null;
            for (Iterator it = startingPoints.keySet().iterator(); it.hasNext(); ) {
                String path = (String) it.next();
                // if the previous path is a prefix of this path then the
                // current path is obsolete
                if (previousPath != null && path.startsWith(previousPath)) {
                    it.remove();
                } else {
                    previousPath = path;
                }
            }

            final Iterator scoreNodes = startingPoints.values().iterator();
            return new AbstractQueryHits() {

                private NodeTraversingQueryHits currentTraversal;

                {
                    fetchNextTraversal();
                }

                public void close() throws IOException {
                    if (currentTraversal != null) {
                        currentTraversal.close();
                    }
                }

                public ScoreNode nextScoreNode() throws IOException {
                    while (currentTraversal != null) {
                        ScoreNode sn = currentTraversal.nextScoreNode();
                        if (sn != null) {
                            return sn;
                        } else {
                            fetchNextTraversal();
                        }
                    }
                    // if we get here there are no more score nodes
                    return null;
                }

                private void fetchNextTraversal() throws IOException {
                    if (currentTraversal != null) {
                        currentTraversal.close();
                    }
                    if (scoreNodes.hasNext()) {
                        ScoreNode sn = (ScoreNode) scoreNodes.next();
                        try {
                            Node node = session.getNodeById(sn.getNodeId());
                            currentTraversal = new NodeTraversingQueryHits(node,
                                    getMinLevels() == 0);
                        } catch (RepositoryException e) {
                            IOException ex = new IOException(e.getMessage());
                            ex.initCause(e);
                            throw ex;
                        }
                    } else {
                        currentTraversal = null;
                    }
                }
            };
        } else {
            return null;
        }
    }

    //--------------------< DescendantSelfAxisWeight >--------------------------
 
     /**
      * The <code>Weight</code> implementation for this
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FilterQueryHits.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FilterQueryHits.java
new file mode 100644
index 000000000..7f987042c
-- /dev/null
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/FilterQueryHits.java
@@ -0,0 +1,69 @@
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

import java.io.IOException;

/**
 * <code>FilterQueryHits</code> implements a {@link QueryHits} filter that
 * forwards each call to the underlying query hits.
 */
public class FilterQueryHits implements QueryHits {

    /**
     * The underlying query hits.
     */
    private final QueryHits hits;

    /**
     * Creates a new <code>FilterQueryHits</code>, which forwards each call to
     * <code>hits</code>.
     *
     * @param hits the underlying query hits.
     */
    public FilterQueryHits(QueryHits hits) {
        this.hits = hits;
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        hits.close();
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        return hits.getSize();
    }

    /**
     * {@inheritDoc}
     */
    public ScoreNode nextScoreNode() throws IOException {
        return hits.nextScoreNode();
    }

    /**
     * {@inheritDoc}
     */
    public void skip(int n) throws IOException {
        hits.skip(n);
    }
}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitIndexSearcher.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitIndexSearcher.java
index 8d6b3556c..db9bd54a1 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitIndexSearcher.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitIndexSearcher.java
@@ -19,11 +19,9 @@ package org.apache.jackrabbit.core.query.lucene;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.Sort;
import org.apache.lucene.search.MatchAllDocsQuery;
 import org.apache.lucene.index.IndexReader;
 import org.apache.jackrabbit.core.SessionImpl;
 
import javax.jcr.RepositoryException;
 import java.io.IOException;
 
 /**
@@ -63,20 +61,14 @@ public class JackrabbitIndexSearcher extends IndexSearcher {
      * @throws IOException if an error occurs while executing the query.
      */
     public QueryHits execute(Query query, Sort sort) throws IOException {
        // optimize certain queries
        if (sort.getSort().length == 0) {
            query = query.rewrite(reader);
            if (query instanceof MatchAllDocsQuery) {
                try {
                    return new NodeTraversingQueryHits(
                            session.getRootNode(), true);
                } catch (RepositoryException e) {
                    IOException ex = new IOException(e.getMessage());
                    ex.initCause(e);
                    throw ex;
                }
            }
        query = query.rewrite(reader);
        QueryHits hits = null;
        if (query instanceof JackrabbitQuery) {
            hits = ((JackrabbitQuery) query).execute(this, session, sort);
         }
        return new LuceneQueryHits(search(query, sort), reader);
        if (hits == null) {
            hits = new LuceneQueryHits(search(query, sort), reader);
        }
        return hits;
     }
 }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitQuery.java
new file mode 100644
index 000000000..32489969e
-- /dev/null
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/JackrabbitQuery.java
@@ -0,0 +1,52 @@
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

import org.apache.lucene.search.Sort;
import org.apache.jackrabbit.core.SessionImpl;

import java.io.IOException;

/**
 * <code>JackrabbitQuery</code> defines an interface for Jackrabbit query
 * implementations that are at the root of the lucene query tree. It gives the
 * implementation the opportunity to execute in an optimized way returning
 * {@link QueryHits} instead of a result that is tied to Lucene.
 */
public interface JackrabbitQuery {

    /**
     * Executes this query and returns {@link QueryHits} or <code>null</code> if
     * this query should be executed using the regular Lucene API.
     * <p/>
     * <b>Important note:</b> an implementation <b>must not</b> call {@link
     * JackrabbitIndexSearcher#execute(Query, Sort)} with this query instance as
     * a parameter, otherwise a stack overflow will occur.
     *
     * @param searcher the jackrabbit index searcher.
     * @param session  the session that executes the query.
     * @param sort     the sort criteria that must be reflected in the returned
     *                 {@link QueryHits}.
     * @return the query hits or <code>null</code> if the regular Lucene API
     *         should be used by the caller.
     * @throws IOException if an error occurs while executing the query.
     */
    public QueryHits execute(JackrabbitIndexSearcher searcher,
                             SessionImpl session,
                             Sort sort)
            throws IOException;
}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryBuilder.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryBuilder.java
index fdf054579..25b29b9e3 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryBuilder.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryBuilder.java
@@ -59,7 +59,6 @@ import org.apache.lucene.search.BooleanClause.Occur;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryHits.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryHits.java
index 05cb66d34..b59245830 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryHits.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryHits.java
@@ -54,13 +54,6 @@ public class LuceneQueryHits extends AbstractQueryHits {
         this.reader = reader;
     }
 
    /**
     * {@inheritDoc}
     */
    protected final void doClose() throws IOException {
        reader.close();
    }

     /**
      * {@inheritDoc}
      */
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllDocsQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllDocsQuery.java
new file mode 100644
index 000000000..2338ad03d
-- /dev/null
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/MatchAllDocsQuery.java
@@ -0,0 +1,52 @@
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

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.lucene.search.Sort;

import javax.jcr.RepositoryException;
import java.io.IOException;

/**
 * <code>MatchAllDocsQuery</code> extends the lucene <code>MatchAllDocsQuery</code>
 * and in addition implements {@link JackrabbitQuery}.
 */
public class MatchAllDocsQuery
        extends org.apache.lucene.search.MatchAllDocsQuery
        implements JackrabbitQuery {

    /**
     * {@inheritDoc}
     */
    public QueryHits execute(JackrabbitIndexSearcher searcher,
                             SessionImpl session,
                             Sort sort) throws IOException {
        if (sort.getSort().length == 0) {
            try {
                return new NodeTraversingQueryHits(
                        session.getRootNode(), true);
            } catch (RepositoryException e) {
                IOException ex = new IOException(e.getMessage());
                ex.initCause(e);
                throw ex;
            }
        } else {
            return null;
        }
    }
}
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NodeTraversingQueryHits.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NodeTraversingQueryHits.java
index f02114038..dd6e5297d 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NodeTraversingQueryHits.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/NodeTraversingQueryHits.java
@@ -24,9 +24,9 @@ import javax.jcr.NodeIterator;
 import javax.jcr.RepositoryException;
 import java.io.IOException;
 import java.util.Iterator;
import java.util.Arrays;
 import java.util.List;
 import java.util.ArrayList;
import java.util.Collections;
 
 /**
  * <code>NodeTraversingQueryHits</code> implements query hits that traverse
@@ -40,36 +40,31 @@ public class NodeTraversingQueryHits extends AbstractQueryHits {
     private final Iterator nodes;
 
     /**
     * Creates query hits that consist of the nodes that are traversed from
     * a given <code>start</code> node.
     * Creates query hits that consist of the nodes that are traversed from a
     * given <code>start</code> node.
      *
     * @param start the start node of the traversal.
     * @param start        the start node of the traversal.
      * @param includeStart whether to include the start node in the result.
      */
     public NodeTraversingQueryHits(Node start, boolean includeStart) {
        this.nodes = new TraversingNodeIterator(start);
        if (!includeStart) {
            nodes.next();
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Does nothing.
     */
    protected void doClose() throws IOException {
        // nothing to do
        this(start, includeStart, Integer.MAX_VALUE);
     }
 
     /**
     * {@inheritDoc}
     * <p/>
     * This implementation always returns <code>-1</code>.
     * Creates query hits that consist of the nodes that are traversed from a
     * given <code>start</code> node.
     *
     * @param start        the start node of the traversal.
     * @param includeStart whether to include the start node in the result.
     * @param maxDepth     the maximum depth of nodes to traverse.
      */
    public int getSize() {
        // don't know
        return -1;
    public NodeTraversingQueryHits(Node start,
                                   boolean includeStart,
                                   int maxDepth) {
        this.nodes = new TraversingNodeIterator(start, maxDepth);
        if (!includeStart) {
            nodes.next();
        }
     }
 
     /**
@@ -96,18 +91,29 @@ public class NodeTraversingQueryHits extends AbstractQueryHits {
          */
         private final Node currentNode;
 
        /**
         * The maximum depth of the traversal.
         */
        private final int maxDepth;

         /**
          * The chain of iterators which includes the iterators of the children
          * of the current node.
          */
        private IteratorChain selfAndChildren;
        private Iterator selfAndChildren;
 
         /**
          * Creates a <code>TraversingNodeIterator</code>.
         * @param start the node from where to start the traversal.
         *
         * @param start    the node from where to start the traversal.
         * @param maxDepth the maximum depth of nodes to traverse.
          */
        TraversingNodeIterator(Node start) {
        TraversingNodeIterator(Node start, int maxDepth) {
            if (maxDepth < 0) {
                throw new IllegalArgumentException("maxDepth must be >= 0");
            }
             currentNode = start;
            this.maxDepth = maxDepth;
         }
 
         /**
@@ -139,18 +145,27 @@ public class NodeTraversingQueryHits extends AbstractQueryHits {
          */
         private void init() {
             if (selfAndChildren == null) {
                Iterator current = Arrays.asList(new Node[]{currentNode}).iterator();
                 List allIterators = new ArrayList();
                Iterator current = Collections.singletonList(currentNode).iterator();
                 allIterators.add(current);

                // create new TraversingNodeIterator for each child
                try {
                    NodeIterator children = currentNode.getNodes();
                    while (children.hasNext()) {
                        allIterators.add(new TraversingNodeIterator(children.nextNode()));
                if (maxDepth == 0) {
                    // only current node
                } else if (maxDepth == 1) {
                    try {
                        allIterators.add(currentNode.getNodes());
                    } catch (RepositoryException e) {
                        // currentNode is probably stale
                    }
                } else {
                    // create new TraversingNodeIterator for each child
                    try {
                        NodeIterator children = currentNode.getNodes();
                        while (children.hasNext()) {
                            allIterators.add(new TraversingNodeIterator(children.nextNode(), maxDepth - 1));
                        }
                    } catch (RepositoryException e) {
                        // currentNode is probably stale
                     }
                } catch (RepositoryException e) {
                    // currentNode is probably stale
                 }
                 selfAndChildren = new IteratorChain(allIterators);
             }
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java
index 053492699..98223eb44 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/SearchIndex.java
@@ -655,8 +655,17 @@ public class SearchIndex extends AbstractQueryHandler {
 
         Sort sort = new Sort(createSortFields(orderProps, orderSpecs));
 
        IndexReader reader = getIndexReader(queryImpl.needsSystemTree());
        return new JackrabbitIndexSearcher(session, reader).execute(query, sort);
        final IndexReader reader = getIndexReader(queryImpl.needsSystemTree());
        return new FilterQueryHits(new JackrabbitIndexSearcher(session, reader).execute(query, sort)) {
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    PerQueryCache.getInstance().dispose();
                    reader.close();
                }
            }
        };
     }
 
     /**
- 
2.19.1.windows.1

