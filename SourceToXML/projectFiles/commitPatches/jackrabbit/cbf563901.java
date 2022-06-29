From cbf563901faef53706d4fa0e4bbf657598351483 Mon Sep 17 00:00:00 2001
From: Marcel Reutegger <mreutegg@apache.org>
Date: Tue, 12 Feb 2008 16:47:25 +0000
Subject: [PATCH] JCR-1365: Query path constraints like foo//*/bar do not scale

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@620859 13f79535-47bb-0310-9956-ffa450edef68
--
 .../core/query/lucene/ChildAxisQuery.java     | 109 ++++++++++++++----
 .../query/lucene/DescendantSelfAxisQuery.java |  79 +++++++++++--
 .../core/query/lucene/LuceneQueryBuilder.java |  18 +--
 3 files changed, 162 insertions(+), 44 deletions(-)

diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ChildAxisQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ChildAxisQuery.java
index 97527ad34..6b0cd85a2 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ChildAxisQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/ChildAxisQuery.java
@@ -20,12 +20,12 @@ import org.apache.jackrabbit.core.NodeId;
 import org.apache.jackrabbit.core.query.LocationStepQueryNode;
 import org.apache.jackrabbit.core.query.lucene.hits.AdaptingHits;
 import org.apache.jackrabbit.core.query.lucene.hits.Hits;
import org.apache.jackrabbit.core.query.lucene.hits.HitsIntersection;
 import org.apache.jackrabbit.core.query.lucene.hits.ScorerHits;
 import org.apache.jackrabbit.core.state.ItemStateException;
 import org.apache.jackrabbit.core.state.ItemStateManager;
 import org.apache.jackrabbit.core.state.NodeState;
 import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.uuid.UUID;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.Term;
@@ -38,12 +38,14 @@ import org.apache.lucene.search.Searcher;
 import org.apache.lucene.search.Similarity;
 import org.apache.lucene.search.TermQuery;
 import org.apache.lucene.search.Weight;
import org.apache.lucene.search.MatchAllDocsQuery;
 
 import java.io.IOException;
import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Set;
import java.util.HashMap;
import java.util.Map;
 
 /**
  * Implements a lucene <code>Query</code> which returns the child nodes of the
@@ -115,6 +117,21 @@ class ChildAxisQuery extends Query {
         this.position = position;
     }
 
    /**
     * @return the name test or <code>null</code> if none was specified.
     */
    String getNameTest() {
        return nameTest;
    }

    /**
     * @return the position check or {@link LocationStepQueryNode#NONE} is none
     *         was specified.
     */
    int getPosition() {
        return position;
    }

     /**
      * Creates a <code>Weight</code> instance for this query.
      *
@@ -137,6 +154,24 @@ class ChildAxisQuery extends Query {
      */
     public Query rewrite(IndexReader reader) throws IOException {
         Query cQuery = contextQuery.rewrite(reader);
        // only try to compact if no position is specified
        if (position == LocationStepQueryNode.NONE) {
            if (cQuery instanceof DescendantSelfAxisQuery) {
                DescendantSelfAxisQuery dsaq = (DescendantSelfAxisQuery) cQuery;
                if (dsaq.subQueryMatchesAll()) {
                    Query sub;
                    if (nameTest == null) {
                        sub = new MatchAllDocsQuery();
                    } else {
                        sub = new TermQuery(new Term(FieldNames.LABEL, nameTest));
                    }
                    return new DescendantSelfAxisQuery(dsaq.getContextQuery(),
                            sub, dsaq.getMinLevels() + 1).rewrite(reader);
                }
            }
        }

        // if we get here we could not compact the query
         if (cQuery == contextQuery) {
             return this;
         } else {
@@ -217,7 +252,8 @@ class ChildAxisQuery extends Query {
             if (nameTest != null) {
                 nameTestScorer = new TermQuery(new Term(FieldNames.LABEL, nameTest)).weight(searcher).scorer(reader);
             }
            return new ChildAxisScorer(searcher.getSimilarity(), reader);
            return new ChildAxisScorer(searcher.getSimilarity(),
                    reader, (HierarchyResolver) reader);
         }
 
         /**
@@ -240,6 +276,11 @@ class ChildAxisQuery extends Query {
          */
         private final IndexReader reader;
 
        /**
         * The <code>HierarchyResolver</code> of the index.
         */
        private final HierarchyResolver hResolver;

         /**
          * The next document id to return
          */
@@ -255,10 +296,14 @@ class ChildAxisQuery extends Query {
          *
          * @param similarity the <code>Similarity</code> instance to use.
          * @param reader     for index access.
         * @param hResolver  the hierarchy resolver of <code>reader</code>.
          */
        protected ChildAxisScorer(Similarity similarity, IndexReader reader) {
        protected ChildAxisScorer(Similarity similarity,
                                  IndexReader reader,
                                  HierarchyResolver hResolver) {
             super(similarity);
             this.reader = reader;
            this.hResolver = hResolver;
         }
 
         /**
@@ -313,7 +358,7 @@ class ChildAxisQuery extends Query {
             if (hits == null) {
 
                 // collect all context nodes
                List uuids = new ArrayList();
                Map uuids = new HashMap();
                 final Hits contextHits = new AdaptingHits();
                 contextScorer.score(new HitCollector() {
                     public void collect(int doc, float score) {
@@ -322,39 +367,53 @@ class ChildAxisQuery extends Query {
                 });
 
                 // read the uuids of the context nodes
                int i = contextHits.next();
                while (i > -1) {
                    String uuid = reader.document(i).get(FieldNames.UUID);
                    uuids.add(uuid);
                    i = contextHits.next();
                for (int i = contextHits.next(); i > -1; i = contextHits.next()) {
                    String uuid = reader.document(i, FieldSelectors.UUID).get(FieldNames.UUID);
                    uuids.put(new Integer(i), uuid);
                 }
 
                 // collect all children of the context nodes
                 Hits childrenHits = new AdaptingHits();

                TermDocs docs = reader.termDocs();
                try {
                    for (Iterator it = uuids.iterator(); it.hasNext();) {
                        docs.seek(new Term(FieldNames.PARENT, (String) it.next()));
                        while (docs.next()) {
                            childrenHits.set(docs.doc());
                if (nameTestScorer != null) {
                    Hits nameHits = new ScorerHits(nameTestScorer);
                    for (int h = nameHits.next(); h > -1; h = nameHits.next()) {
                        if (uuids.containsKey(new Integer(hResolver.getParent(h)))) {
                            childrenHits.set(h);
                         }
                     }
                } finally {
                    docs.close();
                }

                if (nameTestScorer != null) {
                    hits = new HitsIntersection(childrenHits, new ScorerHits(nameTestScorer));
                 } else {
                    hits = childrenHits;
                    // get child node entries for each hit
                    for (Iterator it = uuids.values().iterator(); it.hasNext(); ) {
                        String uuid = (String) it.next();
                        NodeId id = new NodeId(UUID.fromString(uuid));
                        try {
                            NodeState state = (NodeState) itemMgr.getItemState(id);
                            Iterator entries = state.getChildNodeEntries().iterator();
                            while (entries.hasNext()) {
                                NodeId childId = ((NodeState.ChildNodeEntry) entries.next()).getId();
                                Term uuidTerm = new Term(FieldNames.UUID, childId.getUUID().toString());
                                TermDocs docs = reader.termDocs(uuidTerm);
                                try {
                                    if (docs.next()) {
                                        childrenHits.set(docs.doc());
                                    }
                                } finally {
                                    docs.close();
                                }
                            }
                        } catch (ItemStateException e) {
                            // does not exist anymore -> ignore
                        }
                    }
                 }

                hits = childrenHits;
             }
         }
 
         private boolean indexIsValid(int i) throws IOException {
             if (position != LocationStepQueryNode.NONE) {
                Document node = reader.document(i);
                Document node = reader.document(i, FieldSelectors.UUID_AND_PARENT);
                 NodeId parentId = NodeId.valueOf(node.get(FieldNames.PARENT));
                 NodeId id = NodeId.valueOf(node.get(FieldNames.UUID));
                 try {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DescendantSelfAxisQuery.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DescendantSelfAxisQuery.java
index 2b0d3cba3..eac0155d9 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DescendantSelfAxisQuery.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/DescendantSelfAxisQuery.java
@@ -24,6 +24,7 @@ import org.apache.lucene.search.Scorer;
 import org.apache.lucene.search.Searcher;
 import org.apache.lucene.search.Similarity;
 import org.apache.lucene.search.Weight;
import org.apache.lucene.search.MatchAllDocsQuery;
 
 import java.io.IOException;
 import java.util.BitSet;
@@ -52,16 +53,31 @@ class DescendantSelfAxisQuery extends Query {
     private final Query subQuery;
 
     /**
     * If <code>true</code> this query acts on the descendant-or-self axis.
     * If <code>false</code> this query acts on the descendant axis.
     * The minimal levels required between context and sub nodes for a sub node
     * to match.
      */
    private final boolean includeSelf;
    private final int minLevels;
 
     /**
      * The scorer of the sub query to filter
      */
     private Scorer subScorer;
 
    /**
     * Creates a new <code>DescendantSelfAxisQuery</code> based on a
     * <code>context</code> and matches all descendants of the context nodes.
     * Whether the context nodes match as well is controlled by
     * <code>includeSelf</code>.
     *
     * @param context     the context for this query.
     * @param includeSelf if <code>true</code> this query acts like a
     *                    descendant-or-self axis. If <code>false</code> this
     *                    query acts like a descendant axis.
     */
    public DescendantSelfAxisQuery(Query context, boolean includeSelf) {
        this(context, new MatchAllDocsQuery(), includeSelf);
    }

     /**
      * Creates a new <code>DescendantSelfAxisQuery</code> based on a
      * <code>context</code> query and filtering the <code>sub</code> query.
@@ -84,9 +100,45 @@ class DescendantSelfAxisQuery extends Query {
      *                    a descendant axis.
      */
     public DescendantSelfAxisQuery(Query context, Query sub, boolean includeSelf) {
        this(context, sub, includeSelf ? 0 : 1);
    }

    /**
     * Creates a new <code>DescendantSelfAxisQuery</code> based on a
     * <code>context</code> query and filtering the <code>sub</code> query.
     *
     * @param context   the context for this query.
     * @param sub       the sub query.
     * @param minLevels the minimal levels required between context and sub
     *                  nodes for a sub node to match.
     */
    public DescendantSelfAxisQuery(Query context, Query sub, int minLevels) {
         this.contextQuery = context;
         this.subQuery = sub;
        this.includeSelf = includeSelf;
        this.minLevels = minLevels;
    }

    /**
     * @return the context query of this <code>DescendantSelfAxisQuery</code>.
     */
    Query getContextQuery() {
        return contextQuery;
    }

    /**
     * @return <code>true</code> if the sub query of this <code>DescendantSelfAxisQuery</code>
     *         matches all nodes.
     */
    boolean subQueryMatchesAll() {
        return subQuery instanceof MatchAllDocsQuery;
    }

    /**
     * @return the minimal levels required between context and sub nodes for a
     *         sub node to match.
     */
    int getMinLevels() {
        return minLevels;
     }
 
     /**
@@ -123,10 +175,17 @@ class DescendantSelfAxisQuery extends Query {
     public Query rewrite(IndexReader reader) throws IOException {
         Query cQuery = contextQuery.rewrite(reader);
         Query sQuery = subQuery.rewrite(reader);
        if (contextQuery instanceof DescendantSelfAxisQuery) {
            DescendantSelfAxisQuery dsaq = (DescendantSelfAxisQuery) contextQuery;
            if (dsaq.subQueryMatchesAll()) {
                return new DescendantSelfAxisQuery(dsaq.getContextQuery(),
                        sQuery, dsaq.getMinLevels() + getMinLevels()).rewrite(reader);
            }
        }
         if (cQuery == contextQuery && sQuery == subQuery) {
             return this;
         } else {
            return new DescendantSelfAxisQuery(cQuery, sQuery, includeSelf);
            return new DescendantSelfAxisQuery(cQuery, sQuery, minLevels);
         }
     }
 
@@ -153,6 +212,8 @@ class DescendantSelfAxisQuery extends Query {
             this.searcher = searcher;
         }
 
        //-----------------------------< Weight >-------------------------------

         /**
          * Returns this <code>DescendantSelfAxisQuery</code>.
          *
@@ -330,10 +391,8 @@ class DescendantSelfAxisQuery extends Query {
          */
         private boolean isValid(int doc) throws IOException {
             // check self if necessary
            if (includeSelf) {
                if (contextHits.get(doc)) {
                    return true;
                }
            if (minLevels == 0 && contextHits.get(doc)) {
                return true;
             }
 
             // check if doc is a descendant of one of the context nodes
@@ -343,7 +402,7 @@ class DescendantSelfAxisQuery extends Query {
             ancestorDocs[ancestorCount++] = parentDoc;
 
             // traverse
            while (parentDoc != -1 && !contextHits.get(parentDoc)) {
            while (parentDoc != -1 && (!contextHits.get(parentDoc) || ancestorCount < minLevels)) {
                 parentDoc = hResolver.getParent(parentDoc);
                 // resize array if needed
                 if (ancestorCount == ancestorDocs.length) {
diff --git a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryBuilder.java b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryBuilder.java
index 20c765306..b0692094f 100644
-- a/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryBuilder.java
++ b/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/query/lucene/LuceneQueryBuilder.java
@@ -59,6 +59,7 @@ import org.apache.lucene.search.BooleanClause.Occur;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
@@ -556,19 +557,18 @@ public class LuceneQueryBuilder implements QueryNodeVisitor {
                     }
                 } else {
                     // todo this will traverse the whole index, optimize!
                    Query subQuery = null;
                    try {
                        subQuery = createMatchAllQuery(resolver.getJCRName(NameConstants.JCR_PRIMARYTYPE));
                    } catch (NamespaceException e) {
                        // will never happen, prefixes are created when unknown
                    }
                     // only use descendant axis if path is not //*
                     PathQueryNode pathNode = (PathQueryNode) node.getParent();
                     if (pathNode.getPathSteps()[0] != node) {
                        context = new DescendantSelfAxisQuery(context, subQuery);
                        andQuery.add(new ChildAxisQuery(sharedItemMgr, context, null, node.getIndex()), Occur.MUST);
                        if (node.getIndex() == LocationStepQueryNode.NONE) {
                            context = new DescendantSelfAxisQuery(context, false);
                            andQuery.add(context, Occur.MUST);
                        } else {
                            context = new DescendantSelfAxisQuery(context, true);
                            andQuery.add(new ChildAxisQuery(sharedItemMgr, context, null, node.getIndex()), Occur.MUST);
                        }
                     } else {
                        andQuery.add(subQuery, Occur.MUST);
                        andQuery.add(new MatchAllDocsQuery(), Occur.MUST);
                     }
                 }
             }
- 
2.19.1.windows.1

