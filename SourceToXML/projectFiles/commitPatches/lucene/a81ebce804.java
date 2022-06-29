From a81ebce804947685b86b50f7525335120fde38b4 Mon Sep 17 00:00:00 2001
From: Matt Weber <matt@mattweber.org>
Date: Mon, 26 Dec 2016 07:50:58 -0800
Subject: [PATCH] Support Graph Token Streams in QueryBuilder

Adds support for handling graph token streams inside the
QueryBuilder util class used by query parsers.
--
 .../org/apache/lucene/search/GraphQuery.java  | 136 +++++++++++
 .../org/apache/lucene/util/QueryBuilder.java  | 113 +++++++--
 .../graph/GraphTokenStreamFiniteStrings.java  | 230 ++++++++++++++++++
 .../apache/lucene/search/TestGraphQuery.java  |  79 ++++++
 .../apache/lucene/util/TestQueryBuilder.java  |  15 +-
 .../TestGraphTokenStreamFiniteStrings.java    | 217 +++++++++++++++++
 .../queryparser/classic/QueryParserBase.java  |  42 +++-
 .../classic/TestMultiFieldQueryParser.java    |  11 +-
 .../queryparser/classic/TestQueryParser.java  | 131 +++++-----
 9 files changed, 877 insertions(+), 97 deletions(-)
 create mode 100644 lucene/core/src/java/org/apache/lucene/search/GraphQuery.java
 create mode 100644 lucene/core/src/java/org/apache/lucene/util/graph/GraphTokenStreamFiniteStrings.java
 create mode 100644 lucene/core/src/test/org/apache/lucene/search/TestGraphQuery.java
 create mode 100644 lucene/core/src/test/org/apache/lucene/util/graph/TestGraphTokenStreamFiniteStrings.java

diff --git a/lucene/core/src/java/org/apache/lucene/search/GraphQuery.java b/lucene/core/src/java/org/apache/lucene/search/GraphQuery.java
new file mode 100644
index 00000000000..a1308c9cb4c
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/search/GraphQuery.java
@@ -0,0 +1,136 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.lucene.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.lucene.index.IndexReader;

/**
 * A query that wraps multiple sub-queries generated from a graph token stream.
 */
public final class GraphQuery extends Query {
  private final Query[] queries;
  private boolean hasBoolean = false;
  private boolean hasPhrase = false;

  /**
   * Constructor sets the queries and checks if any of them are
   * a boolean query.
   *
   * @param queries the non-null array of queries
   */
  public GraphQuery(Query... queries) {
    this.queries = Objects.requireNonNull(queries).clone();
    for (Query query : queries) {
      if (query instanceof BooleanQuery) {
        hasBoolean = true;
      } else if (query instanceof PhraseQuery) {
        hasPhrase = true;
      }
    }
  }

  /**
   * Gets the queries
   *
   * @return unmodifiable list of Query
   */
  public List<Query> getQueries() {
    return Collections.unmodifiableList(Arrays.asList(queries));
  }

  /**
   * If there is at least one boolean query or not.
   *
   * @return true if there is a boolean, false if not
   */
  public boolean hasBoolean() {
    return hasBoolean;
  }

  /**
   * If there is at least one phrase query or not.
   *
   * @return true if there is a phrase query, false if not
   */
  public boolean hasPhrase() {
    return hasPhrase;
  }

  /**
   * Rewrites to a single query or a boolean query where each query is a SHOULD clause.
   */
  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    if (queries.length == 0) {
      return new BooleanQuery.Builder().build();
    }

    if (queries.length == 1) {
      return queries[0];
    }

    BooleanQuery.Builder q = new BooleanQuery.Builder();
    for (Query clause : queries) {
      q.add(clause, BooleanClause.Occur.SHOULD);
    }

    return q.build();
  }

  @Override
  public String toString(String field) {
    StringBuilder builder = new StringBuilder("Graph(");
    for (int i = 0; i < queries.length; i++) {
      if (i != 0) {
        builder.append(", ");
      }
      builder.append(Objects.toString(queries[i]));
    }

    if (queries.length > 0) {
      builder.append(", ");
    }

    builder.append("hasBoolean=")
        .append(hasBoolean)
        .append(", hasPhrase=")
        .append(hasPhrase)
        .append(")");

    return builder.toString();
  }

  @Override
  public boolean equals(Object other) {
    return sameClassAs(other) &&
        hasBoolean == ((GraphQuery) other).hasBoolean &&
        hasPhrase == ((GraphQuery) other).hasPhrase &&
        Arrays.equals(queries, ((GraphQuery) other).queries);
  }

  @Override
  public int hashCode() {
    return 31 * classHash() + Arrays.deepHashCode(new Object[]{hasBoolean, hasPhrase, queries});
  }
}
\ No newline at end of file
diff --git a/lucene/core/src/java/org/apache/lucene/util/QueryBuilder.java b/lucene/core/src/java/org/apache/lucene/util/QueryBuilder.java
index 6c5ea15aac5..a8c0a82e156 100644
-- a/lucene/core/src/java/org/apache/lucene/util/QueryBuilder.java
++ b/lucene/core/src/java/org/apache/lucene/util/QueryBuilder.java
@@ -25,15 +25,18 @@ import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.CachingTokenFilter;
 import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
 import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.GraphQuery;
 import org.apache.lucene.search.MultiPhraseQuery;
 import org.apache.lucene.search.PhraseQuery;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.SynonymQuery;
 import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.graph.GraphTokenStreamFiniteStrings;
 
 /**
  * Creates queries from the {@link Analyzer} chain.
@@ -135,17 +138,39 @@ public class QueryBuilder {
     
     Query query = createFieldQuery(analyzer, BooleanClause.Occur.SHOULD, field, queryText, false, 0);
     if (query instanceof BooleanQuery) {
      BooleanQuery bq = (BooleanQuery) query;
      BooleanQuery.Builder builder = new BooleanQuery.Builder();
      builder.setMinimumNumberShouldMatch((int) (fraction * bq.clauses().size()));
      for (BooleanClause clause : bq) {
        builder.add(clause);
      query = addMinShouldMatchToBoolean((BooleanQuery) query, fraction);
    } else if (query instanceof GraphQuery && ((GraphQuery) query).hasBoolean()) {
      // we have a graph query that has at least one boolean sub-query
      // re-build and set minimum should match on each boolean found
      List<Query> oldQueries = ((GraphQuery) query).getQueries();
      Query[] queries = new Query[oldQueries.size()];
      for (int i = 0; i < queries.length; i++) {
        Query oldQuery = oldQueries.get(i);
        if (oldQuery instanceof BooleanQuery) {
          queries[i] = addMinShouldMatchToBoolean((BooleanQuery) oldQuery, fraction);
        } else {
          queries[i] = oldQuery;
        }
       }
      query = builder.build();

      query = new GraphQuery(queries);
     }
     return query;
   }
  

  /**
   * Rebuilds a boolean query and sets a new minimum number should match value.
   */
  private BooleanQuery addMinShouldMatchToBoolean(BooleanQuery query, float fraction) {
    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    builder.setMinimumNumberShouldMatch((int) (fraction * query.clauses().size()));
    for (BooleanClause clause : query) {
      builder.add(clause);
    }

    return builder.build();
  }

   /** 
    * Returns the analyzer. 
    * @see #setAnalyzer(Analyzer)
@@ -183,6 +208,7 @@ public class QueryBuilder {
     this.enablePositionIncrements = enable;
   }
 

   /**
    * Creates a query from the analysis chain.
    * <p>
@@ -192,25 +218,44 @@ public class QueryBuilder {
    * it is usually not necessary to override it in a subclass; instead, override
    * methods like {@link #newBooleanQuery}, etc., if possible.
    *
   * @param analyzer analyzer used for this query
   * @param operator default boolean operator used for this query
   * @param field field to create queries against
   * @param queryText text to be passed to the analysis chain
   * @param quoted true if phrases should be generated when terms occur at more than one position
   * @param analyzer   analyzer used for this query
   * @param operator   default boolean operator used for this query
   * @param field      field to create queries against
   * @param queryText  text to be passed to the analysis chain
   * @param quoted     true if phrases should be generated when terms occur at more than one position
    * @param phraseSlop slop factor for phrase/multiphrase queries
    */
   protected Query createFieldQuery(Analyzer analyzer, BooleanClause.Occur operator, String field, String queryText, boolean quoted, int phraseSlop) {
     assert operator == BooleanClause.Occur.SHOULD || operator == BooleanClause.Occur.MUST;
    

     // Use the analyzer to get all the tokens, and then build an appropriate
     // query based on the analysis chain.
    
    try (TokenStream source = analyzer.tokenStream(field, queryText);
         CachingTokenFilter stream = new CachingTokenFilter(source)) {
    try (TokenStream source = analyzer.tokenStream(field, queryText)) {
      return createFieldQuery(source, operator, field, quoted, phraseSlop);
    } catch (IOException e) {
      throw new RuntimeException("Error analyzing query text", e);
    }
  }

  /**
   * Creates a query from a token stream.
   *
   * @param source     the token stream to create the query from
   * @param operator   default boolean operator used for this query
   * @param field      field to create queries against
   * @param quoted     true if phrases should be generated when terms occur at more than one position
   * @param phraseSlop slop factor for phrase/multiphrase queries
   */
  protected Query createFieldQuery(TokenStream source, BooleanClause.Occur operator, String field, boolean quoted, int phraseSlop) {
    assert operator == BooleanClause.Occur.SHOULD || operator == BooleanClause.Occur.MUST;

    // Build an appropriate query based on the analysis chain.
    try (CachingTokenFilter stream = new CachingTokenFilter(source)) {
       
       TermToBytesRefAttribute termAtt = stream.getAttribute(TermToBytesRefAttribute.class);
       PositionIncrementAttribute posIncAtt = stream.addAttribute(PositionIncrementAttribute.class);
      
      PositionLengthAttribute posLenAtt = stream.addAttribute(PositionLengthAttribute.class);

       if (termAtt == null) {
         return null; 
       }
@@ -221,6 +266,7 @@ public class QueryBuilder {
       int numTokens = 0;
       int positionCount = 0;
       boolean hasSynonyms = false;
      boolean isGraph = false;
 
       stream.reset();
       while (stream.incrementToken()) {
@@ -231,6 +277,11 @@ public class QueryBuilder {
         } else {
           hasSynonyms = true;
         }

        int positionLength = posLenAtt.getPositionLength();
        if (!isGraph && positionLength > 1) {
          isGraph = true;
        }
       }
       
       // phase 2: based on token count, presence of synonyms, and options
@@ -241,6 +292,9 @@ public class QueryBuilder {
       } else if (numTokens == 1) {
         // single term
         return analyzeTerm(field, stream);
      } else if (isGraph) {
        // graph
        return analyzeGraph(stream, operator, field, quoted, phraseSlop);
       } else if (quoted && positionCount > 1) {
         // phrase
         if (hasSynonyms) {
@@ -388,7 +442,30 @@ public class QueryBuilder {
     }
     return mpqb.build();
   }
  

  /**
   * Creates a query from a graph token stream by extracting all the finite strings from the graph and using them to create the query.
   */
  protected Query analyzeGraph(TokenStream source, BooleanClause.Occur operator, String field, boolean quoted, int phraseSlop)
      throws IOException {
    source.reset();
    List<TokenStream> tokenStreams = GraphTokenStreamFiniteStrings.getTokenStreams(source);

    if (tokenStreams.isEmpty()) {
      return null;
    }

    List<Query> queries = new ArrayList<>(tokenStreams.size());
    for (TokenStream ts : tokenStreams) {
      Query query = createFieldQuery(ts, operator, field, quoted, phraseSlop);
      if (query != null) {
        queries.add(query);
      }
    }

    return new GraphQuery(queries.toArray(new Query[0]));
  }

   /**
    * Builds a new BooleanQuery instance.
    * <p>
diff --git a/lucene/core/src/java/org/apache/lucene/util/graph/GraphTokenStreamFiniteStrings.java b/lucene/core/src/java/org/apache/lucene/util/graph/GraphTokenStreamFiniteStrings.java
new file mode 100644
index 00000000000..cec65fadfad
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/util/graph/GraphTokenStreamFiniteStrings.java
@@ -0,0 +1,230 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.lucene.util.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.BytesTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.FiniteStringsIterator;
import org.apache.lucene.util.automaton.Operations;

import static org.apache.lucene.util.automaton.Operations.DEFAULT_MAX_DETERMINIZED_STATES;

/**
 * Creates a list of {@link TokenStream} where each stream is the tokens that make up a finite string in graph token stream.  To do this,
 * the graph token stream is converted to an {@link Automaton} and from there we use a {@link FiniteStringsIterator} to collect the various
 * token streams for each finite string.
 */
public final class GraphTokenStreamFiniteStrings {
  private final Automaton.Builder builder = new Automaton.Builder();
  private final Map<BytesRef, Integer> termToID = new HashMap<>();
  private final Map<Integer, BytesRef> idToTerm = new HashMap<>();
  private final Map<Integer, Integer> idToInc = new HashMap<>();
  private Automaton det;

  private class FiniteStringsTokenStream extends TokenStream {
    private final BytesTermAttribute termAtt = addAttribute(BytesTermAttribute.class);
    private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
    private final IntsRef ids;
    private final int end;
    private int offset;

    FiniteStringsTokenStream(final IntsRef ids) {
      assert ids != null;
      this.ids = ids;
      this.offset = ids.offset;
      this.end = ids.offset + ids.length;
    }

    @Override
    public boolean incrementToken() throws IOException {
      if (offset < end) {
        clearAttributes();
        int id = ids.ints[offset];
        termAtt.setBytesRef(idToTerm.get(id));

        int incr = 1;
        if (idToInc.containsKey(id)) {
          incr = idToInc.get(id);
        }
        posIncAtt.setPositionIncrement(incr);
        offset++;
        return true;
      }

      return false;
    }
  }

  private GraphTokenStreamFiniteStrings() {
  }

  /**
   * Gets the list of finite string token streams from the given input graph token stream.
   */
  public static List<TokenStream> getTokenStreams(final TokenStream in) throws IOException {
    GraphTokenStreamFiniteStrings gfs = new GraphTokenStreamFiniteStrings();
    return gfs.process(in);
  }

  /**
   * Builds automaton and builds the finite string token streams.
   */
  private List<TokenStream> process(final TokenStream in) throws IOException {
    build(in);

    List<TokenStream> tokenStreams = new ArrayList<>();
    final FiniteStringsIterator finiteStrings = new FiniteStringsIterator(det);
    for (IntsRef ids; (ids = finiteStrings.next()) != null; ) {
      tokenStreams.add(new FiniteStringsTokenStream(IntsRef.deepCopyOf(ids)));
    }

    return tokenStreams;
  }

  private void build(final TokenStream in) throws IOException {
    if (det != null) {
      throw new IllegalStateException("Automation already built");
    }

    final TermToBytesRefAttribute termBytesAtt = in.addAttribute(TermToBytesRefAttribute.class);
    final PositionIncrementAttribute posIncAtt = in.addAttribute(PositionIncrementAttribute.class);
    final PositionLengthAttribute posLengthAtt = in.addAttribute(PositionLengthAttribute.class);

    in.reset();

    int pos = -1;
    int prevIncr = 1;
    int state = -1;
    while (in.incrementToken()) {
      int currentIncr = posIncAtt.getPositionIncrement();
      if (pos == -1 && currentIncr < 1) {
        throw new IllegalStateException("Malformed TokenStream, start token can't have increment less than 1");
      }

      // always use inc 1 while building, but save original increment
      int incr = Math.min(1, currentIncr);
      if (incr > 0) {
        pos += incr;
      }

      int endPos = pos + posLengthAtt.getPositionLength();
      while (state < endPos) {
        state = createState();
      }

      BytesRef term = termBytesAtt.getBytesRef();
      int id = getTermID(currentIncr, prevIncr, term);
      addTransition(pos, endPos, currentIncr, id);

      // only save last increment on non-zero increment in case we have multiple stacked tokens
      if (currentIncr > 0) {
        prevIncr = currentIncr;
      }
    }

    in.end();
    setAccept(state, true);
    finish();
  }

  /**
   * Returns a new state; state 0 is always the initial state.
   */
  private int createState() {
    return builder.createState();
  }

  /**
   * Marks the specified state as accept or not.
   */
  private void setAccept(int state, boolean accept) {
    builder.setAccept(state, accept);
  }

  /**
   * Adds a transition to the automaton.
   */
  private void addTransition(int source, int dest, int incr, int id) {
    builder.addTransition(source, dest, id);
  }

  /**
   * Call this once you are done adding states/transitions.
   */
  private void finish() {
    finish(DEFAULT_MAX_DETERMINIZED_STATES);
  }

  /**
   * Call this once you are done adding states/transitions.
   *
   * @param maxDeterminizedStates Maximum number of states created when determinizing the automaton.  Higher numbers allow this operation
   *                              to consume more memory but allow more complex automatons.
   */
  private void finish(int maxDeterminizedStates) {
    Automaton automaton = builder.finish();
    det = Operations.removeDeadStates(Operations.determinize(automaton, maxDeterminizedStates));
  }

  /**
   * Gets an integer id for a given term.
   *
   * If there is no position gaps for this token then we can reuse the id for the same term if it appeared at another
   * position without a gap.  If we have a position gap generate a new id so we can keep track of the position
   * increment.
   */
  private int getTermID(int incr, int prevIncr, BytesRef term) {
    assert term != null;
    boolean isStackedGap = incr == 0 && prevIncr > 1;
    boolean hasGap = incr > 1;
    Integer id;
    if (hasGap || isStackedGap) {
      id = idToTerm.size();
      idToTerm.put(id, BytesRef.deepCopyOf(term));

      // stacked token should have the same increment as original token at this position
      if (isStackedGap) {
        idToInc.put(id, prevIncr);
      } else {
        idToInc.put(id, incr);
      }
    } else {
      id = termToID.get(term);
      if (id == null) {
        term = BytesRef.deepCopyOf(term);
        id = idToTerm.size();
        termToID.put(term, id);
        idToTerm.put(id, term);
      }
    }

    return id;
  }
}
\ No newline at end of file
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestGraphQuery.java b/lucene/core/src/test/org/apache/lucene/search/TestGraphQuery.java
new file mode 100644
index 00000000000..412fac4654c
-- /dev/null
++ b/lucene/core/src/test/org/apache/lucene/search/TestGraphQuery.java
@@ -0,0 +1,79 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.search;


import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.util.LuceneTestCase;

public class TestGraphQuery extends LuceneTestCase {

  public void testEquals() {
    QueryUtils.checkEqual(new GraphQuery(), new GraphQuery());
    QueryUtils.checkEqual(new GraphQuery(new MatchAllDocsQuery()), new GraphQuery(new MatchAllDocsQuery()));
    QueryUtils.checkEqual(
        new GraphQuery(new TermQuery(new Term("a", "a")), new TermQuery(new Term("a", "b"))),
        new GraphQuery(new TermQuery(new Term("a", "a")), new TermQuery(new Term("a", "b")))
    );
  }

  public void testBooleanDetection() {
    assertFalse(new GraphQuery().hasBoolean());
    assertFalse(new GraphQuery(new MatchAllDocsQuery(), new TermQuery(new Term("a", "a"))).hasBoolean());
    assertTrue(new GraphQuery(new BooleanQuery.Builder().build()).hasBoolean());
    assertTrue(new GraphQuery(new TermQuery(new Term("a", "a")), new BooleanQuery.Builder().build()).hasBoolean());
  }

  public void testPhraseDetection() {
    assertFalse(new GraphQuery().hasPhrase());
    assertFalse(new GraphQuery(new MatchAllDocsQuery(), new TermQuery(new Term("a", "a"))).hasPhrase());
    assertTrue(new GraphQuery(new PhraseQuery.Builder().build()).hasPhrase());
    assertTrue(new GraphQuery(new TermQuery(new Term("a", "a")), new PhraseQuery.Builder().build()).hasPhrase());
  }

  public void testToString() {
    assertEquals("Graph(hasBoolean=false, hasPhrase=false)", new GraphQuery().toString());
    assertEquals("Graph(a:a, a:b, hasBoolean=true, hasPhrase=false)",
        new GraphQuery(new TermQuery(new Term("a", "a")),
            new BooleanQuery.Builder().add(new TermQuery(new Term("a", "b")), BooleanClause.Occur.SHOULD)
                .build()).toString());
    assertEquals("Graph(a:\"a b\", a:b, hasBoolean=true, hasPhrase=true)",
        new GraphQuery(
            new PhraseQuery.Builder()
                .add(new Term("a", "a"))
                .add(new Term("a", "b")).build(),
            new BooleanQuery.Builder().add(new TermQuery(new Term("a", "b")), BooleanClause.Occur.SHOULD)
                .build()).toString());
  }

  public void testRewrite() throws IOException {
    QueryUtils.checkEqual(new BooleanQuery.Builder().build(), new GraphQuery().rewrite(null));
    QueryUtils.checkEqual(new TermQuery(new Term("a", "a")),
        new GraphQuery(new TermQuery(new Term("a", "a"))).rewrite(null));
    QueryUtils.checkEqual(
        new BooleanQuery.Builder()
            .add(new TermQuery(new Term("a", "a")), BooleanClause.Occur.SHOULD)
            .add(new TermQuery(new Term("b", "b")), BooleanClause.Occur.SHOULD).build(),
        new GraphQuery(
            new TermQuery(new Term("a", "a")),
            new TermQuery(new Term("b", "b"))
        ).rewrite(null)
    );
  }
}
diff --git a/lucene/core/src/test/org/apache/lucene/util/TestQueryBuilder.java b/lucene/core/src/test/org/apache/lucene/util/TestQueryBuilder.java
index d3019e3d077..9cd839027ed 100644
-- a/lucene/core/src/test/org/apache/lucene/util/TestQueryBuilder.java
++ b/lucene/core/src/test/org/apache/lucene/util/TestQueryBuilder.java
@@ -31,6 +31,7 @@ import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.GraphQuery;
 import org.apache.lucene.search.MultiPhraseQuery;
 import org.apache.lucene.search.PhraseQuery;
 import org.apache.lucene.search.Query;
@@ -150,13 +151,17 @@ public class TestQueryBuilder extends LuceneTestCase {
     assertEquals(expectedBuilder.build(), builder.createPhraseQuery("field", "old dogs"));
   }
 
  /** forms multiphrase query */
  /** forms graph query */
   public void testMultiWordSynonymsPhrase() throws Exception {
    MultiPhraseQuery.Builder expectedBuilder = new MultiPhraseQuery.Builder();
    expectedBuilder.add(new Term[] { new Term("field", "guinea"), new Term("field", "cavy") });
    expectedBuilder.add(new Term("field", "pig"));
    PhraseQuery.Builder expectedPhrase = new PhraseQuery.Builder();
    expectedPhrase.add(new Term("field", "guinea"));
    expectedPhrase.add(new Term("field", "pig"));

    TermQuery expectedTerm = new TermQuery(new Term("field", "cavy"));

     QueryBuilder queryBuilder = new QueryBuilder(new MockSynonymAnalyzer());
    assertEquals(expectedBuilder.build(), queryBuilder.createPhraseQuery("field", "guinea pig"));
    assertEquals(new GraphQuery(expectedPhrase.build(), expectedTerm),
        queryBuilder.createPhraseQuery("field", "guinea pig"));
   }
 
   protected static class SimpleCJKTokenizer extends Tokenizer {
diff --git a/lucene/core/src/test/org/apache/lucene/util/graph/TestGraphTokenStreamFiniteStrings.java b/lucene/core/src/test/org/apache/lucene/util/graph/TestGraphTokenStreamFiniteStrings.java
new file mode 100644
index 00000000000..4e636e249dc
-- /dev/null
++ b/lucene/core/src/test/org/apache/lucene/util/graph/TestGraphTokenStreamFiniteStrings.java
@@ -0,0 +1,217 @@
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.util.graph;

import java.util.List;

import org.apache.lucene.analysis.CannedTokenStream;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.BytesTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.LuceneTestCase;

/**
 * {@link GraphTokenStreamFiniteStrings} tests.
 */
public class TestGraphTokenStreamFiniteStrings extends LuceneTestCase {

  private static Token token(String term, int posInc, int posLength) {
    final Token t = new Token(term, 0, term.length());
    t.setPositionIncrement(posInc);
    t.setPositionLength(posLength);
    return t;
  }

  private void assertTokenStream(TokenStream ts, String[] terms, int[] increments) throws Exception {
    // verify no nulls and arrays same length
    assertNotNull(ts);
    assertNotNull(terms);
    assertNotNull(increments);
    assertEquals(terms.length, increments.length);
    BytesTermAttribute termAtt = ts.getAttribute(BytesTermAttribute.class);
    PositionIncrementAttribute incrAtt = ts.getAttribute(PositionIncrementAttribute.class);
    int offset = 0;
    while (ts.incrementToken()) {
      // verify term and increment
      assert offset < terms.length;
      assertEquals(terms[offset], termAtt.getBytesRef().utf8ToString());
      assertEquals(increments[offset], incrAtt.getPositionIncrement());
      offset++;
    }

    // make sure we processed all items
    assertEquals(offset, terms.length);
  }

  public void testIllegalState() throws Exception {
    expectThrows(IllegalStateException.class, () -> {
      TokenStream ts = new CannedTokenStream(
          token("a", 0, 1),
          token("b", 1, 1)
      );

      GraphTokenStreamFiniteStrings.getTokenStreams(ts);
    });
  }

  public void testSingleGraph() throws Exception {
    TokenStream ts = new CannedTokenStream(
        token("fast", 1, 1),
        token("wi", 1, 1),
        token("wifi", 0, 2),
        token("fi", 1, 1),
        token("network", 1, 1)
    );

    List<TokenStream> finiteTokenStreams = GraphTokenStreamFiniteStrings.getTokenStreams(ts);

    assertEquals(2, finiteTokenStreams.size());
    assertTokenStream(finiteTokenStreams.get(0), new String[]{"fast", "wi", "fi", "network"}, new int[]{1, 1, 1, 1});
    assertTokenStream(finiteTokenStreams.get(1), new String[]{"fast", "wifi", "network"}, new int[]{1, 1, 1});
  }

  public void testSingleGraphWithGap() throws Exception {
    // "hey the fast wifi network", where "the" removed
    TokenStream ts = new CannedTokenStream(
        token("hey", 1, 1),
        token("fast", 2, 1),
        token("wi", 1, 1),
        token("wifi", 0, 2),
        token("fi", 1, 1),
        token("network", 1, 1)
    );

    List<TokenStream> finiteTokenStreams = GraphTokenStreamFiniteStrings.getTokenStreams(ts);

    assertEquals(2, finiteTokenStreams.size());
    assertTokenStream(finiteTokenStreams.get(0),
        new String[]{"hey", "fast", "wi", "fi", "network"}, new int[]{1, 2, 1, 1, 1});
    assertTokenStream(finiteTokenStreams.get(1),
        new String[]{"hey", "fast", "wifi", "network"}, new int[]{1, 2, 1, 1});
  }


  public void testGraphAndGapSameToken() throws Exception {
    TokenStream ts = new CannedTokenStream(
        token("fast", 1, 1),
        token("wi", 2, 1),
        token("wifi", 0, 2),
        token("fi", 1, 1),
        token("network", 1, 1)
    );

    List<TokenStream> finiteTokenStreams = GraphTokenStreamFiniteStrings.getTokenStreams(ts);

    assertEquals(2, finiteTokenStreams.size());
    assertTokenStream(finiteTokenStreams.get(0), new String[]{"fast", "wi", "fi", "network"}, new int[]{1, 2, 1, 1});
    assertTokenStream(finiteTokenStreams.get(1), new String[]{"fast", "wifi", "network"}, new int[]{1, 2, 1});
  }

  public void testGraphAndGapSameTokenTerm() throws Exception {
    TokenStream ts = new CannedTokenStream(
        token("a", 1, 1),
        token("b", 1, 1),
        token("c", 2, 1),
        token("a", 0, 2),
        token("d", 1, 1)
    );

    List<TokenStream> finiteTokenStreams = GraphTokenStreamFiniteStrings.getTokenStreams(ts);

    assertEquals(2, finiteTokenStreams.size());
    assertTokenStream(finiteTokenStreams.get(0), new String[]{"a", "b", "c", "d"}, new int[]{1, 1, 2, 1});
    assertTokenStream(finiteTokenStreams.get(1), new String[]{"a", "b", "a"}, new int[]{1, 1, 2});
  }

  public void testStackedGraph() throws Exception {
    TokenStream ts = new CannedTokenStream(
        token("fast", 1, 1),
        token("wi", 1, 1),
        token("wifi", 0, 2),
        token("wireless", 0, 2),
        token("fi", 1, 1),
        token("network", 1, 1)
    );

    List<TokenStream> finiteTokenStreams = GraphTokenStreamFiniteStrings.getTokenStreams(ts);

    assertEquals(3, finiteTokenStreams.size());
    assertTokenStream(finiteTokenStreams.get(0), new String[]{"fast", "wi", "fi", "network"}, new int[]{1, 1, 1, 1});
    assertTokenStream(finiteTokenStreams.get(1), new String[]{"fast", "wifi", "network"}, new int[]{1, 1, 1});
    assertTokenStream(finiteTokenStreams.get(2), new String[]{"fast", "wireless", "network"}, new int[]{1, 1, 1});
  }

  public void testStackedGraphWithGap() throws Exception {
    TokenStream ts = new CannedTokenStream(
        token("fast", 1, 1),
        token("wi", 2, 1),
        token("wifi", 0, 2),
        token("wireless", 0, 2),
        token("fi", 1, 1),
        token("network", 1, 1)
    );

    List<TokenStream> finiteTokenStreams = GraphTokenStreamFiniteStrings.getTokenStreams(ts);

    assertEquals(3, finiteTokenStreams.size());
    assertTokenStream(finiteTokenStreams.get(0), new String[]{"fast", "wi", "fi", "network"}, new int[]{1, 2, 1, 1});
    assertTokenStream(finiteTokenStreams.get(1), new String[]{"fast", "wifi", "network"}, new int[]{1, 2, 1});
    assertTokenStream(finiteTokenStreams.get(2), new String[]{"fast", "wireless", "network"}, new int[]{1, 2, 1});
  }

  public void testGraphWithRegularSynonym() throws Exception {
    TokenStream ts = new CannedTokenStream(
        token("fast", 1, 1),
        token("speedy", 0, 1),
        token("wi", 1, 1),
        token("wifi", 0, 2),
        token("fi", 1, 1),
        token("network", 1, 1)
    );

    List<TokenStream> finiteTokenStreams = GraphTokenStreamFiniteStrings.getTokenStreams(ts);

    assertEquals(4, finiteTokenStreams.size());
    assertTokenStream(finiteTokenStreams.get(0), new String[]{"fast", "wi", "fi", "network"}, new int[]{1, 1, 1, 1});
    assertTokenStream(finiteTokenStreams.get(1), new String[]{"fast", "wifi", "network"}, new int[]{1, 1, 1});
    assertTokenStream(finiteTokenStreams.get(2), new String[]{"speedy", "wi", "fi", "network"}, new int[]{1, 1, 1, 1});
    assertTokenStream(finiteTokenStreams.get(3), new String[]{"speedy", "wifi", "network"}, new int[]{1, 1, 1});
  }

  public void testMultiGraph() throws Exception {
    TokenStream ts = new CannedTokenStream(
        token("turbo", 1, 1),
        token("fast", 0, 2),
        token("charged", 1, 1),
        token("wi", 1, 1),
        token("wifi", 0, 2),
        token("fi", 1, 1),
        token("network", 1, 1)
    );

    List<TokenStream> finiteTokenStreams = GraphTokenStreamFiniteStrings.getTokenStreams(ts);

    assertEquals(4, finiteTokenStreams.size());
    assertTokenStream(finiteTokenStreams.get(0),
        new String[]{"turbo", "charged", "wi", "fi", "network"}, new int[]{1, 1, 1, 1, 1});
    assertTokenStream(finiteTokenStreams.get(1),
        new String[]{"turbo", "charged", "wifi", "network"}, new int[]{1, 1, 1, 1});
    assertTokenStream(finiteTokenStreams.get(2), new String[]{"fast", "wi", "fi", "network"}, new int[]{1, 1, 1, 1});
    assertTokenStream(finiteTokenStreams.get(3), new String[]{"fast", "wifi", "network"}, new int[]{1, 1, 1});
  }
}
diff --git a/lucene/queryparser/src/java/org/apache/lucene/queryparser/classic/QueryParserBase.java b/lucene/queryparser/src/java/org/apache/lucene/queryparser/classic/QueryParserBase.java
index 41d3764f0ac..9b238d87eff 100644
-- a/lucene/queryparser/src/java/org/apache/lucene/queryparser/classic/QueryParserBase.java
++ b/lucene/queryparser/src/java/org/apache/lucene/queryparser/classic/QueryParserBase.java
@@ -475,8 +475,6 @@ public abstract class QueryParserBase extends QueryBuilder implements CommonQuer
     return createFieldQuery(analyzer, occur, field, queryText, quoted || autoGeneratePhraseQueries, phraseSlop);
   }
 


   /**
    * Base implementation delegates to {@link #getFieldQuery(String,String,boolean)}.
    * This method may be overridden, for example, to return
@@ -489,26 +487,48 @@ public abstract class QueryParserBase extends QueryBuilder implements CommonQuer
     Query query = getFieldQuery(field, queryText, true);
 
     if (query instanceof PhraseQuery) {
      PhraseQuery.Builder builder = new PhraseQuery.Builder();
      builder.setSlop(slop);
      PhraseQuery pq = (PhraseQuery) query;
      org.apache.lucene.index.Term[] terms = pq.getTerms();
      int[] positions = pq.getPositions();
      for (int i = 0; i < terms.length; ++i) {
        builder.add(terms[i], positions[i]);
      }
      query = builder.build();
      query = addSlopToPhrase((PhraseQuery) query, slop);
     } else if (query instanceof MultiPhraseQuery) {
       MultiPhraseQuery mpq = (MultiPhraseQuery)query;
       
       if (slop != mpq.getSlop()) {
         query = new MultiPhraseQuery.Builder(mpq).setSlop(slop).build();
       }
    } else if (query instanceof GraphQuery && ((GraphQuery) query).hasPhrase()) {
      // we have a graph query that has at least one phrase sub-query
      // re-build and set slop on all phrase queries
      List<Query> oldQueries = ((GraphQuery) query).getQueries();
      Query[] queries = new Query[oldQueries.size()];
      for (int i = 0; i < queries.length; i++) {
        Query oldQuery = oldQueries.get(i);
        if (oldQuery instanceof PhraseQuery) {
          queries[i] = addSlopToPhrase((PhraseQuery) oldQuery, slop);
        } else {
          queries[i] = oldQuery;
        }
      }

      query = new GraphQuery(queries);
     }
 
     return query;
   }
 
  /**
   * Rebuild a phrase query with a slop value
   */
  private PhraseQuery addSlopToPhrase(PhraseQuery query, int slop) {
    PhraseQuery.Builder builder = new PhraseQuery.Builder();
    builder.setSlop(slop);
    org.apache.lucene.index.Term[] terms = query.getTerms();
    int[] positions = query.getPositions();
    for (int i = 0; i < terms.length; ++i) {
      builder.add(terms[i], positions[i]);
    }

    return builder.build();
  }

   protected Query getRangeQuery(String field,
                                 String part1,
                                 String part2,
diff --git a/lucene/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiFieldQueryParser.java b/lucene/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiFieldQueryParser.java
index ed76ff5009f..ae15284d45b 100644
-- a/lucene/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiFieldQueryParser.java
++ b/lucene/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiFieldQueryParser.java
@@ -21,15 +21,19 @@ import java.io.StringReader;
 import java.util.HashMap;
 import java.util.Map;
 
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.MockSynonymFilter;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.index.DirectoryReader;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
 import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.Query;
@@ -347,7 +351,8 @@ public class TestMultiFieldQueryParser extends LuceneTestCase {
     assertEquals("Synonym(b:dog b:dogs) Synonym(t:dog t:dogs)", q.toString());
     q = parser.parse("guinea pig");
     assertFalse(parser.getSplitOnWhitespace());
    assertEquals("(Synonym(b:cavy b:guinea) Synonym(t:cavy t:guinea)) (b:pig t:pig)", q.toString());
    assertEquals("Graph(b:guinea b:pig, b:cavy, hasBoolean=true, hasPhrase=false) "
        + "Graph(t:guinea t:pig, t:cavy, hasBoolean=true, hasPhrase=false)", q.toString());
     parser.setSplitOnWhitespace(true);
     q = parser.parse("guinea pig");
     assertEquals("(b:guinea t:guinea) (b:pig t:pig)", q.toString());
diff --git a/lucene/queryparser/src/test/org/apache/lucene/queryparser/classic/TestQueryParser.java b/lucene/queryparser/src/test/org/apache/lucene/queryparser/classic/TestQueryParser.java
index bb976249bf6..87bc89fa4f7 100644
-- a/lucene/queryparser/src/test/org/apache/lucene/queryparser/classic/TestQueryParser.java
++ b/lucene/queryparser/src/test/org/apache/lucene/queryparser/classic/TestQueryParser.java
@@ -16,6 +16,8 @@
  */
 package org.apache.lucene.queryparser.classic;
 
import java.io.IOException;

 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.analysis.MockBytesAnalyzer;
@@ -27,10 +29,10 @@ import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.document.DateTools.Resolution;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.DateTools.Resolution;
 import org.apache.lucene.index.DirectoryReader;
 import org.apache.lucene.index.IndexOptions;
 import org.apache.lucene.index.RandomIndexWriter;
@@ -41,6 +43,7 @@ import org.apache.lucene.queryparser.util.QueryParserTestBase;
 import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.GraphQuery;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.MatchAllDocsQuery;
 import org.apache.lucene.search.MultiPhraseQuery;
@@ -51,8 +54,6 @@ import org.apache.lucene.search.TermQuery;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.util.automaton.TooComplexToDeterminizeException;
 
import java.io.IOException;

 /**
  * Tests QueryParser.
  */
@@ -502,32 +503,34 @@ public class TestQueryParser extends QueryParserTestBase {
     QueryParser dumb = new QueryParser("field", new Analyzer1());
     dumb.setSplitOnWhitespace(false);
 
    // A multi-word synonym source will form a synonym query for the same-starting-position tokens
    BooleanQuery.Builder multiWordExpandedBqBuilder = new BooleanQuery.Builder();
    Query multiWordSynonymQuery = new SynonymQuery(new Term("field", "guinea"), new Term("field", "cavy"));
    multiWordExpandedBqBuilder.add(multiWordSynonymQuery, BooleanClause.Occur.SHOULD);
    multiWordExpandedBqBuilder.add(new TermQuery(new Term("field", "pig")), BooleanClause.Occur.SHOULD);
    Query multiWordExpandedBq = multiWordExpandedBqBuilder.build();
    assertEquals(multiWordExpandedBq, dumb.parse("guinea pig"));

    // With the phrase operator, a multi-word synonym source will form a multiphrase query.
    // When the number of expanded term(s) is different from that of the original term(s), this is not good.
    MultiPhraseQuery.Builder multiWordExpandedMpqBuilder = new MultiPhraseQuery.Builder();
    multiWordExpandedMpqBuilder.add(new Term[]{new Term("field", "guinea"), new Term("field", "cavy")});
    multiWordExpandedMpqBuilder.add(new Term("field", "pig"));
    Query multiWordExpandedMPQ = multiWordExpandedMpqBuilder.build();
    assertEquals(multiWordExpandedMPQ, dumb.parse("\"guinea pig\""));
    TermQuery guinea = new TermQuery(new Term("field", "guinea"));
    TermQuery pig = new TermQuery(new Term("field", "pig"));
    TermQuery cavy = new TermQuery(new Term("field", "cavy"));

    // A multi-word synonym source will form a graph query for synonyms that formed the graph token stream
    BooleanQuery.Builder synonym = new BooleanQuery.Builder();
    synonym.add(guinea, BooleanClause.Occur.SHOULD);
    synonym.add(pig, BooleanClause.Occur.SHOULD);
    BooleanQuery guineaPig = synonym.build();

    GraphQuery graphQuery = new GraphQuery(guineaPig, cavy);
    assertEquals(graphQuery, dumb.parse("guinea pig"));

    // With the phrase operator, a multi-word synonym source will form a graph query with inner phrase queries.
    PhraseQuery.Builder phraseSynonym = new PhraseQuery.Builder();
    phraseSynonym.add(new Term("field", "guinea"));
    phraseSynonym.add(new Term("field", "pig"));
    PhraseQuery guineaPigPhrase = phraseSynonym.build();

    graphQuery = new GraphQuery(guineaPigPhrase, cavy);
    assertEquals(graphQuery, dumb.parse("\"guinea pig\""));
 
     // custom behavior, the synonyms are expanded, unless you use quote operator
     QueryParser smart = new SmartQueryParser();
     smart.setSplitOnWhitespace(false);
    assertEquals(multiWordExpandedBq, smart.parse("guinea pig"));

    PhraseQuery.Builder multiWordUnexpandedPqBuilder = new PhraseQuery.Builder();
    multiWordUnexpandedPqBuilder.add(new Term("field", "guinea"));
    multiWordUnexpandedPqBuilder.add(new Term("field", "pig"));
    Query multiWordUnexpandedPq = multiWordUnexpandedPqBuilder.build();
    assertEquals(multiWordUnexpandedPq, smart.parse("\"guinea pig\""));
    graphQuery = new GraphQuery(guineaPig, cavy);
    assertEquals(graphQuery, smart.parse("guinea pig"));
    assertEquals(guineaPigPhrase, smart.parse("\"guinea pig\""));
   }
 
   // TODO: Move to QueryParserTestBase once standard flexible parser gets this capability
@@ -580,34 +583,34 @@ public class TestQueryParser extends QueryParserTestBase {
     assertQueryEquals("guinea /pig/", a, "guinea /pig/");
 
     // Operators should not interrupt multiword analysis if not don't associate
    assertQueryEquals("(guinea pig)", a, "Synonym(cavy guinea) pig");
    assertQueryEquals("+(guinea pig)", a, "+(Synonym(cavy guinea) pig)");
    assertQueryEquals("-(guinea pig)", a, "-(Synonym(cavy guinea) pig)");
    assertQueryEquals("!(guinea pig)", a, "-(Synonym(cavy guinea) pig)");
    assertQueryEquals("NOT (guinea pig)", a, "-(Synonym(cavy guinea) pig)");
    assertQueryEquals("(guinea pig)^2", a, "(Synonym(cavy guinea) pig)^2.0");

    assertQueryEquals("field:(guinea pig)", a, "Synonym(cavy guinea) pig");

    assertQueryEquals("+small guinea pig", a, "+small Synonym(cavy guinea) pig");
    assertQueryEquals("-small guinea pig", a, "-small Synonym(cavy guinea) pig");
    assertQueryEquals("!small guinea pig", a, "-small Synonym(cavy guinea) pig");
    assertQueryEquals("NOT small guinea pig", a, "-small Synonym(cavy guinea) pig");
    assertQueryEquals("small* guinea pig", a, "small* Synonym(cavy guinea) pig");
    assertQueryEquals("small? guinea pig", a, "small? Synonym(cavy guinea) pig");
    assertQueryEquals("\"small\" guinea pig", a, "small Synonym(cavy guinea) pig");

    assertQueryEquals("guinea pig +running", a, "Synonym(cavy guinea) pig +running");
    assertQueryEquals("guinea pig -running", a, "Synonym(cavy guinea) pig -running");
    assertQueryEquals("guinea pig !running", a, "Synonym(cavy guinea) pig -running");
    assertQueryEquals("guinea pig NOT running", a, "Synonym(cavy guinea) pig -running");
    assertQueryEquals("guinea pig running*", a, "Synonym(cavy guinea) pig running*");
    assertQueryEquals("guinea pig running?", a, "Synonym(cavy guinea) pig running?");
    assertQueryEquals("guinea pig \"running\"", a, "Synonym(cavy guinea) pig running");

    assertQueryEquals("\"guinea pig\"~2", a, "\"(guinea cavy) pig\"~2");

    assertQueryEquals("field:\"guinea pig\"", a, "\"(guinea cavy) pig\"");
    assertQueryEquals("(guinea pig)", a, "Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false)");
    assertQueryEquals("+(guinea pig)", a, "+Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false)");
    assertQueryEquals("-(guinea pig)", a, "-Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false)");
    assertQueryEquals("!(guinea pig)", a, "-Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false)");
    assertQueryEquals("NOT (guinea pig)", a, "-Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false)");
    assertQueryEquals("(guinea pig)^2", a, "(Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false))^2.0");

    assertQueryEquals("field:(guinea pig)", a, "Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false)");

    assertQueryEquals("+small guinea pig", a, "+small Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false)");
    assertQueryEquals("-small guinea pig", a, "-small Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false)");
    assertQueryEquals("!small guinea pig", a, "-small Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false)");
    assertQueryEquals("NOT small guinea pig", a, "-small Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false)");
    assertQueryEquals("small* guinea pig", a, "small* Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false)");
    assertQueryEquals("small? guinea pig", a, "small? Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false)");
    assertQueryEquals("\"small\" guinea pig", a, "small Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false)");

    assertQueryEquals("guinea pig +running", a, "Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false) +running");
    assertQueryEquals("guinea pig -running", a, "Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false) -running");
    assertQueryEquals("guinea pig !running", a, "Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false) -running");
    assertQueryEquals("guinea pig NOT running", a, "Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false) -running");
    assertQueryEquals("guinea pig running*", a, "Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false) running*");
    assertQueryEquals("guinea pig running?", a, "Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false) running?");
    assertQueryEquals("guinea pig \"running\"", a, "Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false) running");

    assertQueryEquals("\"guinea pig\"~2", a, "Graph(field:\"guinea pig\"~2, field:cavy, hasBoolean=false, hasPhrase=true)");

    assertQueryEquals("field:\"guinea pig\"", a, "Graph(field:\"guinea pig\", field:cavy, hasBoolean=false, hasPhrase=true)");
 
     splitOnWhitespace = oldSplitOnWhitespace;
   }
@@ -684,9 +687,9 @@ public class TestQueryParser extends QueryParserTestBase {
     assertQueryEquals("guinea pig running?", a, "guinea pig running?");
     assertQueryEquals("guinea pig \"running\"", a, "guinea pig running");
 
    assertQueryEquals("\"guinea pig\"~2", a, "\"(guinea cavy) pig\"~2");
    assertQueryEquals("\"guinea pig\"~2", a, "Graph(field:\"guinea pig\"~2, field:cavy, hasBoolean=false, hasPhrase=true)");
 
    assertQueryEquals("field:\"guinea pig\"", a, "\"(guinea cavy) pig\"");
    assertQueryEquals("field:\"guinea pig\"", a, "Graph(field:\"guinea pig\", field:cavy, hasBoolean=false, hasPhrase=true)");
 
     splitOnWhitespace = oldSplitOnWhitespace;
   }
@@ -697,14 +700,22 @@ public class TestQueryParser extends QueryParserTestBase {
     assertFalse(parser.getSplitOnWhitespace()); // default is false
 
     // A multi-word synonym source will form a synonym query for the same-starting-position tokens
    BooleanQuery.Builder bqBuilder = new BooleanQuery.Builder();
    bqBuilder.add(new SynonymQuery(new Term("field", "guinea"), new Term("field", "cavy")), BooleanClause.Occur.SHOULD);
    bqBuilder.add(new TermQuery(new Term("field", "pig")), BooleanClause.Occur.SHOULD);
    assertEquals(bqBuilder.build(), parser.parse("guinea pig"));
    TermQuery guinea = new TermQuery(new Term("field", "guinea"));
    TermQuery pig = new TermQuery(new Term("field", "pig"));
    TermQuery cavy = new TermQuery(new Term("field", "cavy"));

    // A multi-word synonym source will form a graph query for synonyms that formed the graph token stream
    BooleanQuery.Builder synonym = new BooleanQuery.Builder();
    synonym.add(guinea, BooleanClause.Occur.SHOULD);
    synonym.add(pig, BooleanClause.Occur.SHOULD);
    BooleanQuery guineaPig = synonym.build();

    GraphQuery graphQuery = new GraphQuery(guineaPig, cavy);
    assertEquals(graphQuery, parser.parse("guinea pig"));
 
     boolean oldSplitOnWhitespace = splitOnWhitespace;
     splitOnWhitespace = QueryParser.DEFAULT_SPLIT_ON_WHITESPACE;
    assertQueryEquals("guinea pig", new MockSynonymAnalyzer(), "Synonym(cavy guinea) pig");
    assertQueryEquals("guinea pig", new MockSynonymAnalyzer(), "Graph(field:guinea field:pig, field:cavy, hasBoolean=true, hasPhrase=false)");
     splitOnWhitespace = oldSplitOnWhitespace;
   }
    
- 
2.19.1.windows.1

