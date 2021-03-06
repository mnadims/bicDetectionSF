From 5a2d0bc654ba52c8499b7de2db88ecdbaaa98d74 Mon Sep 17 00:00:00 2001
From: Robert Muir <rmuir@apache.org>
Date: Wed, 18 Jul 2012 21:04:18 +0000
Subject: [PATCH] LUCENE-2686, LUCENE-3505: Fix various bugs in BooleanQuery,
 clean up scorer navigation API

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1363115 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |  10 +
 .../apache/lucene/search/BooleanQuery.java    |  44 +---
 .../apache/lucene/search/BooleanScorer.java   |   8 +-
 .../apache/lucene/search/BooleanScorer2.java  |  15 +-
 .../lucene/search/ConjunctionScorer.java      |  15 ++
 .../lucene/search/ConjunctionTermScorer.java  |  35 ++-
 .../lucene/search/ConstantScoreQuery.java     |   5 +
 .../lucene/search/DisjunctionMaxQuery.java    |   2 +-
 .../lucene/search/DisjunctionMaxScorer.java   | 102 ++------
 .../lucene/search/DisjunctionScorer.java      | 108 +++++++++
 .../lucene/search/DisjunctionSumScorer.java   | 208 ++++++-----------
 .../apache/lucene/search/FilteredQuery.java   |  10 +
 .../lucene/search/MatchAllDocsQuery.java      |   5 +
 .../MatchOnlyConjunctionTermsScorer.java      |  35 ---
 .../lucene/search/MatchOnlyTermScorer.java    |  20 +-
 .../apache/lucene/search/ReqExclScorer.java   |  13 +-
 .../apache/lucene/search/ReqOptSumScorer.java |  18 ++
 .../search/ScoreCachingWrappingScorer.java    |  13 +-
 .../java/org/apache/lucene/search/Scorer.java |   4 +-
 .../org/apache/lucene/search/TermQuery.java   |  13 +-
 .../org/apache/lucene/search/TermScorer.java  |  19 +-
 .../apache/lucene/util/ScorerDocQueue.java    | 219 ------------------
 .../lucene/search/JustCompileSearch.java      |   5 +
 .../TestBooleanQueryVisitSubscorers.java      | 184 +++++++++++++++
 .../lucene/search/TestBooleanScorer.java      |   1 +
 .../lucene/search/TestCachingCollector.java   |   3 +
 .../lucene/search/TestConjunctions.java       | 142 ++++++++++++
 .../TestPositiveScoresOnlyCollector.java      |   4 +
 .../TestScoreCachingWrappingScorer.java       |   4 +
 .../lucene/search/TestSubScorerFreqs.java     |  10 +-
 .../grouping/BlockGroupingCollector.java      |   5 +
 .../search/join/TermsIncludingScoreQuery.java |   5 +
 .../search/join/ToChildBlockJoinQuery.java    |  10 +-
 .../join/ToParentBlockJoinCollector.java      |   5 +
 .../search/join/ToParentBlockJoinQuery.java   |  16 +-
 .../lucene/queries/CustomScoreQuery.java      |  12 +
 .../lucene/queries/function/BoostedQuery.java |  12 +
 .../queries/function/FunctionQuery.java       |   5 +
 .../queries/function/ValueSourceScorer.java   |   5 +
 .../org/apache/solr/schema/LatLonType.java    |   5 +
 .../apache/solr/search/JoinQParserPlugin.java |   5 +
 .../solr/search/SolrConstantScoreQuery.java   |   5 +
 42 files changed, 810 insertions(+), 554 deletions(-)
 create mode 100644 lucene/core/src/java/org/apache/lucene/search/DisjunctionScorer.java
 delete mode 100644 lucene/core/src/java/org/apache/lucene/search/MatchOnlyConjunctionTermsScorer.java
 delete mode 100755 lucene/core/src/java/org/apache/lucene/util/ScorerDocQueue.java
 create mode 100644 lucene/core/src/test/org/apache/lucene/search/TestBooleanQueryVisitSubscorers.java
 create mode 100644 lucene/core/src/test/org/apache/lucene/search/TestConjunctions.java

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index c9fdf4e1ada..63546c6ac6c 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -74,6 +74,16 @@ Bug Fixes
 * LUCENE-4222: TieredMergePolicy.getFloorSegmentMB was returning the
   size in bytes not MB (Chris Fuller via Mike McCandless)
 
* LUCENE-3505: Fix bug (Lucene 4.0alpha only) where boolean conjunctions
  were sometimes scored incorrectly. Conjunctions of only termqueries where
  at least one term omitted term frequencies (IndexOptions.DOCS_ONLY) would 
  be scored as if all terms omitted term frequencies.  (Robert Muir)

* LUCENE-2686, LUCENE-3505: Fixed BooleanQuery scorers to return correct 
  freq().  Added support for scorer navigation API (Scorer.getChildren) to 
  all queries.  Made Scorer.freq() abstract. 
  (Koji Sekiguchi, Mike McCandless, Robert Muir)

 Build
 
 * LUCENE-4094: Support overriding file.encoding on forked test JVMs
diff --git a/lucene/core/src/java/org/apache/lucene/search/BooleanQuery.java b/lucene/core/src/java/org/apache/lucene/search/BooleanQuery.java
index 7ea93939383..421cc6cfa27 100644
-- a/lucene/core/src/java/org/apache/lucene/search/BooleanQuery.java
++ b/lucene/core/src/java/org/apache/lucene/search/BooleanQuery.java
@@ -358,51 +358,19 @@ public class BooleanQuery extends Query implements Iterable<BooleanClause> {
       final DocsAndFreqs[] docsAndFreqs = new DocsAndFreqs[weights.size()];
       for (int i = 0; i < docsAndFreqs.length; i++) {
         final TermWeight weight = (TermWeight) weights.get(i);
        final TermsEnum termsEnum = weight.getTermsEnum(context);
        if (termsEnum == null) {
        final Scorer scorer = weight.scorer(context, true, false, acceptDocs);
        if (scorer == null) {
           return null;
         }
        final ExactSimScorer docScorer = weight.createDocScorer(context);
        final DocsEnum docsAndFreqsEnum = termsEnum.docs(acceptDocs, null, true);
        if (docsAndFreqsEnum == null) {
          // TODO: we could carry over TermState from the
          // terms we already seek'd to, to save re-seeking
          // to make the match-only scorer, but it's likely
          // rare that BQ mixes terms from omitTf and
          // non-omitTF fields:

          // At least one sub cannot provide freqs; abort
          // and fallback to full match-only scorer:
          return createMatchOnlyConjunctionTermScorer(context, acceptDocs);
        if (scorer instanceof TermScorer) {
          docsAndFreqs[i] = new DocsAndFreqs((TermScorer) scorer);
        } else {
          docsAndFreqs[i] = new DocsAndFreqs((MatchOnlyTermScorer) scorer);
         }

        docsAndFreqs[i] = new DocsAndFreqs(docsAndFreqsEnum,
                                           docsAndFreqsEnum,
                                           termsEnum.docFreq(), docScorer);
       }
       return new ConjunctionTermScorer(this, disableCoord ? 1.0f : coord(
           docsAndFreqs.length, docsAndFreqs.length), docsAndFreqs);
     }

    private Scorer createMatchOnlyConjunctionTermScorer(AtomicReaderContext context, Bits acceptDocs)
        throws IOException {

      final DocsAndFreqs[] docsAndFreqs = new DocsAndFreqs[weights.size()];
      for (int i = 0; i < docsAndFreqs.length; i++) {
        final TermWeight weight = (TermWeight) weights.get(i);
        final TermsEnum termsEnum = weight.getTermsEnum(context);
        if (termsEnum == null) {
          return null;
        }
        final ExactSimScorer docScorer = weight.createDocScorer(context);
        docsAndFreqs[i] = new DocsAndFreqs(null,
                                           termsEnum.docs(acceptDocs, null, false),
                                           termsEnum.docFreq(), docScorer);
      }

      return new MatchOnlyConjunctionTermScorer(this, disableCoord ? 1.0f : coord(
          docsAndFreqs.length, docsAndFreqs.length), docsAndFreqs);
    }
     
     @Override
     public boolean scoresDocsOutOfOrder() {
diff --git a/lucene/core/src/java/org/apache/lucene/search/BooleanScorer.java b/lucene/core/src/java/org/apache/lucene/search/BooleanScorer.java
index 94796f66427..9ea088f7ed0 100644
-- a/lucene/core/src/java/org/apache/lucene/search/BooleanScorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/BooleanScorer.java
@@ -317,6 +317,11 @@ final class BooleanScorer extends Scorer {
     throw new UnsupportedOperationException();
   }
 
  @Override
  public float freq() throws IOException {
    return current.coord;
  }

   @Override
   public void score(Collector collector) throws IOException {
     score(collector, Integer.MAX_VALUE, -1);
@@ -338,7 +343,8 @@ final class BooleanScorer extends Scorer {
   public Collection<ChildScorer> getChildren() {
     List<ChildScorer> children = new ArrayList<ChildScorer>();
     for (SubScorer sub = scorers; sub != null; sub = sub.next) {
      children.add(new ChildScorer(sub.scorer, sub.prohibited ? Occur.MUST_NOT.toString() : Occur.SHOULD.toString()));
      // TODO: fix this if BQ ever sends us required clauses
      children.add(new ChildScorer(sub.scorer, sub.prohibited ? "MUST_NOT" : "SHOULD"));
     }
     return children;
   }
diff --git a/lucene/core/src/java/org/apache/lucene/search/BooleanScorer2.java b/lucene/core/src/java/org/apache/lucene/search/BooleanScorer2.java
index d019e12b24c..4a86f075891 100644
-- a/lucene/core/src/java/org/apache/lucene/search/BooleanScorer2.java
++ b/lucene/core/src/java/org/apache/lucene/search/BooleanScorer2.java
@@ -130,6 +130,11 @@ class BooleanScorer2 extends Scorer {
       return lastDocScore;
     }
 
    @Override
    public float freq() throws IOException {
      return 1;
    }

     @Override
     public int docID() {
       return scorer.docID();
@@ -310,8 +315,8 @@ class BooleanScorer2 extends Scorer {
   }
 
   @Override
  public float freq() {
    return coordinator.nrMatchers;
  public float freq() throws IOException {
    return countingSumScorer.freq();
   }
 
   @Override
@@ -323,13 +328,13 @@ class BooleanScorer2 extends Scorer {
   public Collection<ChildScorer> getChildren() {
     ArrayList<ChildScorer> children = new ArrayList<ChildScorer>();
     for (Scorer s : optionalScorers) {
      children.add(new ChildScorer(s, Occur.SHOULD.toString()));
      children.add(new ChildScorer(s, "SHOULD"));
     }
     for (Scorer s : prohibitedScorers) {
      children.add(new ChildScorer(s, Occur.MUST_NOT.toString()));
      children.add(new ChildScorer(s, "MUST_NOT"));
     }
     for (Scorer s : requiredScorers) {
      children.add(new ChildScorer(s, Occur.MUST.toString()));
      children.add(new ChildScorer(s, "MUST"));
     }
     return children;
   }
diff --git a/lucene/core/src/java/org/apache/lucene/search/ConjunctionScorer.java b/lucene/core/src/java/org/apache/lucene/search/ConjunctionScorer.java
index 8d56f59d3b2..99bf5d4b48c 100644
-- a/lucene/core/src/java/org/apache/lucene/search/ConjunctionScorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/ConjunctionScorer.java
@@ -19,6 +19,7 @@ package org.apache.lucene.search;
 
 import org.apache.lucene.util.ArrayUtil;
 import java.io.IOException;
import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Comparator;
 
@@ -136,4 +137,18 @@ class ConjunctionScorer extends Scorer {
     }
     return sum * coord;
   }

  @Override
  public float freq() throws IOException {
    return scorers.length;
  }

  @Override
  public Collection<ChildScorer> getChildren() {
    ArrayList<ChildScorer> children = new ArrayList<ChildScorer>(scorers.length);
    for (Scorer scorer : scorers) {
      children.add(new ChildScorer(scorer, "MUST"));
    }
    return children;
  }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/search/ConjunctionTermScorer.java b/lucene/core/src/java/org/apache/lucene/search/ConjunctionTermScorer.java
index 3832661d696..cd71e5d3048 100644
-- a/lucene/core/src/java/org/apache/lucene/search/ConjunctionTermScorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/ConjunctionTermScorer.java
@@ -18,10 +18,11 @@ package org.apache.lucene.search;
  */
 
 import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
 import java.util.Comparator;
 
 import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.search.similarities.Similarity.ExactSimScorer;
 import org.apache.lucene.util.ArrayUtil;
 
 /** Scorer for conjunctions, sets of terms, all of which are required. */
@@ -91,23 +92,43 @@ class ConjunctionTermScorer extends Scorer {
   public float score() throws IOException {
     float sum = 0.0f;
     for (DocsAndFreqs docs : docsAndFreqs) {
      sum += docs.docScorer.score(lastDoc, docs.docs.freq());
      sum += docs.scorer.score();
     }
     return sum * coord;
   }
  
  @Override
  public float freq() {
    return docsAndFreqs.length;
  }

  @Override
  public Collection<ChildScorer> getChildren() {
    ArrayList<ChildScorer> children = new ArrayList<ChildScorer>(docsAndFreqs.length);
    for (DocsAndFreqs docs : docsAndFreqs) {
      children.add(new ChildScorer(docs.scorer, "MUST"));
    }
    return children;
  }
 
   static final class DocsAndFreqs {
    final DocsEnum docsAndFreqs;
     final DocsEnum docs;
     final int docFreq;
    final ExactSimScorer docScorer;
    final Scorer scorer;
     int doc = -1;
 
    DocsAndFreqs(DocsEnum docsAndFreqs, DocsEnum docs, int docFreq, ExactSimScorer docScorer) {
      this.docsAndFreqs = docsAndFreqs;
    DocsAndFreqs(TermScorer termScorer) {
      this(termScorer, termScorer.getDocsEnum(), termScorer.getDocFreq());
    }
    
    DocsAndFreqs(MatchOnlyTermScorer termScorer) {
      this(termScorer, termScorer.getDocsEnum(), termScorer.getDocFreq());
    }
    
    DocsAndFreqs(Scorer scorer, DocsEnum docs, int docFreq) {
       this.docs = docs;
       this.docFreq = docFreq;
      this.docScorer = docScorer;
      this.scorer = scorer;
     }
   }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/search/ConstantScoreQuery.java b/lucene/core/src/java/org/apache/lucene/search/ConstantScoreQuery.java
index 31be3523f3c..93ecefea8f3 100644
-- a/lucene/core/src/java/org/apache/lucene/search/ConstantScoreQuery.java
++ b/lucene/core/src/java/org/apache/lucene/search/ConstantScoreQuery.java
@@ -193,6 +193,11 @@ public class ConstantScoreQuery extends Query {
       return theScore;
     }
 
    @Override
    public float freq() throws IOException {
      return 1;
    }

     @Override
     public int advance(int target) throws IOException {
       return docIdSetIterator.advance(target);
diff --git a/lucene/core/src/java/org/apache/lucene/search/DisjunctionMaxQuery.java b/lucene/core/src/java/org/apache/lucene/search/DisjunctionMaxQuery.java
index 87c7ae833d2..2c28ee09271 100644
-- a/lucene/core/src/java/org/apache/lucene/search/DisjunctionMaxQuery.java
++ b/lucene/core/src/java/org/apache/lucene/search/DisjunctionMaxQuery.java
@@ -158,7 +158,7 @@ public class DisjunctionMaxQuery extends Query implements Iterable<Query> {
       for (Weight w : weights) {
         // we will advance() subscorers
         Scorer subScorer = w.scorer(context, true, false, acceptDocs);
        if (subScorer != null && subScorer.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
        if (subScorer != null) {
           scorers[idx++] = subScorer;
         }
       }
diff --git a/lucene/core/src/java/org/apache/lucene/search/DisjunctionMaxScorer.java b/lucene/core/src/java/org/apache/lucene/search/DisjunctionMaxScorer.java
index 88eec00dc27..c5c73277960 100644
-- a/lucene/core/src/java/org/apache/lucene/search/DisjunctionMaxScorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/DisjunctionMaxScorer.java
@@ -17,9 +17,6 @@ package org.apache.lucene.search;
  */
 
 import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
 
 /**
  * The Scorer for DisjunctionMaxQuery.  The union of all documents generated by the the subquery scorers
@@ -27,11 +24,7 @@ import java.util.Collections;
  * by the subquery scorers that generate that document, plus tieBreakerMultiplier times the sum of the scores
  * for the other subqueries that generate the document.
  */
class DisjunctionMaxScorer extends Scorer {

  /* The scorers for subqueries that have remaining docs, kept as a min heap by number of next doc. */
  private final Scorer[] subScorers;
  private int numScorers;
class DisjunctionMaxScorer extends DisjunctionScorer {
   /* Multiplier applied to non-maximum-scoring subqueries for a document as they are summed into the result. */
   private final float tieBreakerMultiplier;
   private int doc = -1;
@@ -56,15 +49,8 @@ class DisjunctionMaxScorer extends Scorer {
    */
   public DisjunctionMaxScorer(Weight weight, float tieBreakerMultiplier,
       Scorer[] subScorers, int numScorers) {
    super(weight);
    super(weight, subScorers, numScorers);
     this.tieBreakerMultiplier = tieBreakerMultiplier;
    // The passed subScorers array includes only scorers which have documents
    // (DisjunctionMaxQuery takes care of that), and their nextDoc() was already
    // called.
    this.subScorers = subScorers;
    this.numScorers = numScorers;
    
    heapify();
   }
 
   @Override
@@ -113,6 +99,24 @@ class DisjunctionMaxScorer extends Scorer {
     }
   }
 
  @Override
  public float freq() throws IOException {
    int doc = subScorers[0].docID();
    int size = numScorers;
    return 1 + freq(1, size, doc) + freq(2, size, doc);
  }
  
  // Recursively iterate all subScorers that generated last doc computing sum and max
  private int freq(int root, int size, int doc) throws IOException {
    int freq = 0;
    if (root < size && subScorers[root].docID() == doc) {
      freq++;
      freq += freq((root<<1)+1, size, doc);
      freq += freq((root<<1)+2, size, doc);
    }
    return freq;
  }

   @Override
   public int advance(int target) throws IOException {
     if (numScorers == 0) return doc = NO_MORE_DOCS;
@@ -128,70 +132,4 @@ class DisjunctionMaxScorer extends Scorer {
     }
     return doc = subScorers[0].docID();
   }

  // Organize subScorers into a min heap with scorers generating the earliest document on top.
  private void heapify() {
    for (int i = (numScorers >> 1) - 1; i >= 0; i--) {
      heapAdjust(i);
    }
  }

  /* The subtree of subScorers at root is a min heap except possibly for its root element.
   * Bubble the root down as required to make the subtree a heap.
   */
  private void heapAdjust(int root) {
    Scorer scorer = subScorers[root];
    int doc = scorer.docID();
    int i = root;
    while (i <= (numScorers >> 1) - 1) {
      int lchild = (i << 1) + 1;
      Scorer lscorer = subScorers[lchild];
      int ldoc = lscorer.docID();
      int rdoc = Integer.MAX_VALUE, rchild = (i << 1) + 2;
      Scorer rscorer = null;
      if (rchild < numScorers) {
        rscorer = subScorers[rchild];
        rdoc = rscorer.docID();
      }
      if (ldoc < doc) {
        if (rdoc < ldoc) {
          subScorers[i] = rscorer;
          subScorers[rchild] = scorer;
          i = rchild;
        } else {
          subScorers[i] = lscorer;
          subScorers[lchild] = scorer;
          i = lchild;
        }
      } else if (rdoc < doc) {
        subScorers[i] = rscorer;
        subScorers[rchild] = scorer;
        i = rchild;
      } else {
        return;
      }
    }
  }

  // Remove the root Scorer from subScorers and re-establish it as a heap
  private void heapRemoveRoot() {
    if (numScorers == 1) {
      subScorers[0] = null;
      numScorers = 0;
    } else {
      subScorers[0] = subScorers[numScorers - 1];
      subScorers[numScorers - 1] = null;
      --numScorers;
      heapAdjust(0);
    }
  }

  @Override
  public Collection<ChildScorer> getChildren() {
    final ChildScorer[] children = new ChildScorer[numScorers];
    for (int i = 0; i< numScorers; i++) {
      children[i] = new ChildScorer(subScorers[i], BooleanClause.Occur.SHOULD.toString());
    }
    return Collections.unmodifiableCollection(Arrays.asList(children));
  }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/search/DisjunctionScorer.java b/lucene/core/src/java/org/apache/lucene/search/DisjunctionScorer.java
new file mode 100644
index 00000000000..84bf866b6c9
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/search/DisjunctionScorer.java
@@ -0,0 +1,108 @@
package org.apache.lucene.search;

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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Base class for Scorers that score disjunctions.
 * Currently this just provides helper methods to manage the heap.
 */
abstract class DisjunctionScorer extends Scorer {
  protected final Scorer subScorers[];
  protected int numScorers;
  
  protected DisjunctionScorer(Weight weight, Scorer subScorers[], int numScorers) {
    super(weight);
    this.subScorers = subScorers;
    this.numScorers = numScorers;
    heapify();
  }
  
  /** 
   * Organize subScorers into a min heap with scorers generating the earliest document on top.
   */
  protected final void heapify() {
    for (int i = (numScorers >> 1) - 1; i >= 0; i--) {
      heapAdjust(i);
    }
  }
  
  /** 
   * The subtree of subScorers at root is a min heap except possibly for its root element.
   * Bubble the root down as required to make the subtree a heap.
   */
  protected final void heapAdjust(int root) {
    Scorer scorer = subScorers[root];
    int doc = scorer.docID();
    int i = root;
    while (i <= (numScorers >> 1) - 1) {
      int lchild = (i << 1) + 1;
      Scorer lscorer = subScorers[lchild];
      int ldoc = lscorer.docID();
      int rdoc = Integer.MAX_VALUE, rchild = (i << 1) + 2;
      Scorer rscorer = null;
      if (rchild < numScorers) {
        rscorer = subScorers[rchild];
        rdoc = rscorer.docID();
      }
      if (ldoc < doc) {
        if (rdoc < ldoc) {
          subScorers[i] = rscorer;
          subScorers[rchild] = scorer;
          i = rchild;
        } else {
          subScorers[i] = lscorer;
          subScorers[lchild] = scorer;
          i = lchild;
        }
      } else if (rdoc < doc) {
        subScorers[i] = rscorer;
        subScorers[rchild] = scorer;
        i = rchild;
      } else {
        return;
      }
    }
  }

  /** 
   * Remove the root Scorer from subScorers and re-establish it as a heap
   */
  protected final void heapRemoveRoot() {
    if (numScorers == 1) {
      subScorers[0] = null;
      numScorers = 0;
    } else {
      subScorers[0] = subScorers[numScorers - 1];
      subScorers[numScorers - 1] = null;
      --numScorers;
      heapAdjust(0);
    }
  }
  
  @Override
  public final Collection<ChildScorer> getChildren() {
    ArrayList<ChildScorer> children = new ArrayList<ChildScorer>(numScorers);
    for (int i = 0; i < numScorers; i++) {
      children.add(new ChildScorer(subScorers[i], "SHOULD"));
    }
    return children;
  }
}
diff --git a/lucene/core/src/java/org/apache/lucene/search/DisjunctionSumScorer.java b/lucene/core/src/java/org/apache/lucene/search/DisjunctionSumScorer.java
index c328e0fd2d8..148fd153d02 100644
-- a/lucene/core/src/java/org/apache/lucene/search/DisjunctionSumScorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/DisjunctionSumScorer.java
@@ -20,42 +20,20 @@ package org.apache.lucene.search;
 import java.util.List;
 import java.io.IOException;
 
import org.apache.lucene.util.ScorerDocQueue;

 /** A Scorer for OR like queries, counterpart of <code>ConjunctionScorer</code>.
  * This Scorer implements {@link Scorer#advance(int)} and uses advance() on the given Scorers. 
  */
class DisjunctionSumScorer extends Scorer {
  /** The number of subscorers. */ 
  private final int nrScorers;
  
  /** The subscorers. */
  protected final List<Scorer> subScorers;
  
class DisjunctionSumScorer extends DisjunctionScorer { 
   /** The minimum number of scorers that should match. */
   private final int minimumNrMatchers;
   
  /** The scorerDocQueue contains all subscorers ordered by their current doc(),
   * with the minimum at the top.
   * <br>The scorerDocQueue is initialized the first time nextDoc() or advance() is called.
   * <br>An exhausted scorer is immediately removed from the scorerDocQueue.
   * <br>If less than the minimumNrMatchers scorers
   * remain in the scorerDocQueue nextDoc() and advance() return false.
   * <p>
   * After each to call to nextDoc() or advance()
   * <code>currentSumScore</code> is the total score of the current matching doc,
   * <code>nrMatchers</code> is the number of matching scorers,
   * and all scorers are after the matching doc, or are exhausted.
   */
  private final ScorerDocQueue scorerDocQueue;
  
   /** The document number of the current match. */
  private int currentDoc = -1;
  private int doc = -1;
 
   /** The number of subscorers that provide the current match. */
   protected int nrMatchers = -1;
 
  private double currentScore = Float.NaN;
  private double score = Float.NaN;
   
   /** Construct a <code>DisjunctionScorer</code>.
    * @param weight The weight to be used.
@@ -69,21 +47,16 @@ class DisjunctionSumScorer extends Scorer {
    * it more efficient to use <code>ConjunctionScorer</code>.
    */
   public DisjunctionSumScorer(Weight weight, List<Scorer> subScorers, int minimumNrMatchers) throws IOException {
    super(weight);
    
    nrScorers = subScorers.size();
    super(weight, subScorers.toArray(new Scorer[subScorers.size()]), subScorers.size());
 
     if (minimumNrMatchers <= 0) {
       throw new IllegalArgumentException("Minimum nr of matchers must be positive");
     }
    if (nrScorers <= 1) {
    if (numScorers <= 1) {
       throw new IllegalArgumentException("There must be at least 2 subScorers");
     }
 
     this.minimumNrMatchers = minimumNrMatchers;
    this.subScorers = subScorers;

    scorerDocQueue  = initScorerDocQueue();
   }
   
   /** Construct a <code>DisjunctionScorer</code>, using one as the minimum number
@@ -93,119 +66,66 @@ class DisjunctionSumScorer extends Scorer {
     this(weight, subScorers, 1);
   }
 
  /** Called the first time nextDoc() or advance() is called to
   * initialize <code>scorerDocQueue</code>.
   * @return 
   */
  private ScorerDocQueue initScorerDocQueue() throws IOException {
    final ScorerDocQueue docQueue = new ScorerDocQueue(nrScorers);
    for (final Scorer se : subScorers) {
      if (se.nextDoc() != NO_MORE_DOCS) {
        docQueue.insert(se);
      }
    }
    return docQueue; 
  }

  /** Scores and collects all matching documents.
   * @param collector The collector to which all matching documents are passed through.
   */
  @Override
  public void score(Collector collector) throws IOException {
    collector.setScorer(this);
    while (nextDoc() != NO_MORE_DOCS) {
      collector.collect(currentDoc);
    }
  }

  /** Expert: Collects matching documents in a range.  Hook for optimization.
   * Note that {@link #nextDoc()} must be called once before this method is called
   * for the first time.
   * @param collector The collector to which all matching documents are passed through.
   * @param max Do not score documents past this.
   * @return true if more matching documents may remain.
   */
  @Override
  public boolean score(Collector collector, int max, int firstDocID) throws IOException {
    // firstDocID is ignored since nextDoc() sets 'currentDoc'
    collector.setScorer(this);
    while (currentDoc < max) {
      collector.collect(currentDoc);
      if (nextDoc() == NO_MORE_DOCS) {
        return false;
      }
    }
    return true;
  }

   @Override
   public int nextDoc() throws IOException {
    
    if (scorerDocQueue.size() < minimumNrMatchers || !advanceAfterCurrent()) {
      currentDoc = NO_MORE_DOCS;
    }
    return currentDoc;
  }

  /** Advance all subscorers after the current document determined by the
   * top of the <code>scorerDocQueue</code>.
   * Repeat until at least the minimum number of subscorers match on the same
   * document and all subscorers are after that document or are exhausted.
   * <br>On entry the <code>scorerDocQueue</code> has at least <code>minimumNrMatchers</code>
   * available. At least the scorer with the minimum document number will be advanced.
   * @return true iff there is a match.
   * <br>In case there is a match, </code>currentDoc</code>, </code>currentSumScore</code>,
   * and </code>nrMatchers</code> describe the match.
   *
   * TODO: Investigate whether it is possible to use advance() when
   * the minimum number of matchers is bigger than one, ie. try and use the
   * character of ConjunctionScorer for the minimum number of matchers.
   * Also delay calling score() on the sub scorers until the minimum number of
   * matchers is reached.
   * <br>For this, a Scorer array with minimumNrMatchers elements might
   * hold Scorers at currentDoc that are temporarily popped from scorerQueue.
   */
  protected boolean advanceAfterCurrent() throws IOException {
    do { // repeat until minimum nr of matchers
      currentDoc = scorerDocQueue.topDoc();
      currentScore = scorerDocQueue.topScore();
      nrMatchers = 1;
      do { // Until all subscorers are after currentDoc
        if (!scorerDocQueue.topNextAndAdjustElsePop()) {
          if (scorerDocQueue.size() == 0) {
            break; // nothing more to advance, check for last match.
    while(true) {
      while (subScorers[0].docID() == doc) {
        if (subScorers[0].nextDoc() != NO_MORE_DOCS) {
          heapAdjust(0);
        } else {
          heapRemoveRoot();
          if (numScorers < minimumNrMatchers) {
            return doc = NO_MORE_DOCS;
           }
         }
        if (scorerDocQueue.topDoc() != currentDoc) {
          break; // All remaining subscorers are after currentDoc.
        }
        currentScore += scorerDocQueue.topScore();
        nrMatchers++;
      } while (true);
      
      }
      afterNext();
       if (nrMatchers >= minimumNrMatchers) {
        return true;
      } else if (scorerDocQueue.size() < minimumNrMatchers) {
        return false;
        break;
       }
    } while (true);
    }
    
    return doc;
  }
  
  private void afterNext() throws IOException {
    final Scorer sub = subScorers[0];
    doc = sub.docID();
    score = sub.score();
    nrMatchers = 1;
    countMatches(1);
    countMatches(2);
  }
  
  // TODO: this currently scores, but so did the previous impl
  // TODO: remove recursion.
  // TODO: if we separate scoring, out of here, modify this
  // and afterNext() to terminate when nrMatchers == minimumNrMatchers
  // then also change freq() to just always compute it from scratch
  private void countMatches(int root) throws IOException {
    if (root < numScorers && subScorers[root].docID() == doc) {
      nrMatchers++;
      score += subScorers[root].score();
      countMatches((root<<1)+1);
      countMatches((root<<1)+2);
    }
   }
   
   /** Returns the score of the current document matching the query.
    * Initially invalid, until {@link #nextDoc()} is called the first time.
    */
   @Override
  public float score() throws IOException { return (float)currentScore; }
  public float score() throws IOException { 
    return (float)score; 
  }
    
   @Override
   public int docID() {
    return currentDoc;
    return doc;
   }
  
  /** Returns the number of subscorers matching the current document.
   * Initially invalid, until {@link #nextDoc()} is called the first time.
   */
  public int nrMatchers() {

  @Override
  public float freq() throws IOException {
     return nrMatchers;
   }
 
@@ -221,20 +141,24 @@ class DisjunctionSumScorer extends Scorer {
    */
   @Override
   public int advance(int target) throws IOException {
    if (scorerDocQueue.size() < minimumNrMatchers) {
      return currentDoc = NO_MORE_DOCS;
    }
    if (target <= currentDoc) {
      return currentDoc;
    }
    do {
      if (scorerDocQueue.topDoc() >= target) {
        return advanceAfterCurrent() ? currentDoc : (currentDoc = NO_MORE_DOCS);
      } else if (!scorerDocQueue.topSkipToAndAdjustElsePop(target)) {
        if (scorerDocQueue.size() < minimumNrMatchers) {
          return currentDoc = NO_MORE_DOCS;
    if (numScorers == 0) return doc = NO_MORE_DOCS;
    while (subScorers[0].docID() < target) {
      if (subScorers[0].advance(target) != NO_MORE_DOCS) {
        heapAdjust(0);
      } else {
        heapRemoveRoot();
        if (numScorers == 0) {
          return doc = NO_MORE_DOCS;
         }
       }
    } while (true);
    }
    
    afterNext();

    if (nrMatchers >= minimumNrMatchers) {
      return doc;
    } else {
      return nextDoc();
    }
   }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/search/FilteredQuery.java b/lucene/core/src/java/org/apache/lucene/search/FilteredQuery.java
index 9cfd3373eea..e2302f974f3 100644
-- a/lucene/core/src/java/org/apache/lucene/search/FilteredQuery.java
++ b/lucene/core/src/java/org/apache/lucene/search/FilteredQuery.java
@@ -24,6 +24,8 @@ import org.apache.lucene.util.Bits;
 import org.apache.lucene.util.ToStringUtils;
 
 import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
 import java.util.Set;
 
 
@@ -221,6 +223,14 @@ public class FilteredQuery extends Query {
             public float score() throws IOException {
               return scorer.score();
             }
            
            @Override
            public float freq() throws IOException { return scorer.freq(); }
            
            @Override
            public Collection<ChildScorer> getChildren() {
              return Collections.singleton(new ChildScorer(scorer, "FILTERED"));
            }
           };
         }
       }
diff --git a/lucene/core/src/java/org/apache/lucene/search/MatchAllDocsQuery.java b/lucene/core/src/java/org/apache/lucene/search/MatchAllDocsQuery.java
index dc3dbd3b726..5844c93ff60 100644
-- a/lucene/core/src/java/org/apache/lucene/search/MatchAllDocsQuery.java
++ b/lucene/core/src/java/org/apache/lucene/search/MatchAllDocsQuery.java
@@ -67,6 +67,11 @@ public class MatchAllDocsQuery extends Query {
       return score;
     }
 
    @Override
    public float freq() {
      return 1;
    }

     @Override
     public int advance(int target) throws IOException {
       doc = target-1;
diff --git a/lucene/core/src/java/org/apache/lucene/search/MatchOnlyConjunctionTermsScorer.java b/lucene/core/src/java/org/apache/lucene/search/MatchOnlyConjunctionTermsScorer.java
deleted file mode 100644
index a8336f26461..00000000000
-- a/lucene/core/src/java/org/apache/lucene/search/MatchOnlyConjunctionTermsScorer.java
++ /dev/null
@@ -1,35 +0,0 @@
package org.apache.lucene.search;

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

/** Scorer for conjunctions, sets of terms, all of which are required. */
final class MatchOnlyConjunctionTermScorer extends ConjunctionTermScorer {
  MatchOnlyConjunctionTermScorer(Weight weight, float coord,
      DocsAndFreqs[] docsAndFreqs) {
    super(weight, coord, docsAndFreqs);
  }

  @Override
  public float score() {
    float sum = 0.0f;
    for (DocsAndFreqs docs : docsAndFreqs) {
      sum += docs.docScorer.score(lastDoc, 1);
    }
    return sum * coord;
  }
}
diff --git a/lucene/core/src/java/org/apache/lucene/search/MatchOnlyTermScorer.java b/lucene/core/src/java/org/apache/lucene/search/MatchOnlyTermScorer.java
index faad389b775..742f0be5147 100644
-- a/lucene/core/src/java/org/apache/lucene/search/MatchOnlyTermScorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/MatchOnlyTermScorer.java
@@ -30,6 +30,7 @@ import org.apache.lucene.search.similarities.Similarity;
 final class MatchOnlyTermScorer extends Scorer {
   private final DocsEnum docsEnum;
   private final Similarity.ExactSimScorer docScorer;
  private final int docFreq;
   
   /**
    * Construct a <code>TermScorer</code>.
@@ -41,11 +42,14 @@ final class MatchOnlyTermScorer extends Scorer {
    * @param docScorer
    *          The </code>Similarity.ExactSimScorer</code> implementation 
    *          to be used for score computations.
   * @param docFreq
   *          per-segment docFreq of this term
    */
  MatchOnlyTermScorer(Weight weight, DocsEnum td, Similarity.ExactSimScorer docScorer) {
  MatchOnlyTermScorer(Weight weight, DocsEnum td, Similarity.ExactSimScorer docScorer, int docFreq) {
     super(weight);
     this.docScorer = docScorer;
     this.docsEnum = td;
    this.docFreq = docFreq;
   }
 
   @Override
@@ -91,4 +95,18 @@ final class MatchOnlyTermScorer extends Scorer {
   /** Returns a string representation of this <code>TermScorer</code>. */
   @Override
   public String toString() { return "scorer(" + weight + ")"; }
  
  // TODO: benchmark if the specialized conjunction really benefits
  // from these, or if instead its from sorting by docFreq, or both

  DocsEnum getDocsEnum() {
    return docsEnum;
  }
  
  // TODO: generalize something like this for scorers?
  // even this is just an estimation...
  
  int getDocFreq() {
    return docFreq;
  }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/search/ReqExclScorer.java b/lucene/core/src/java/org/apache/lucene/search/ReqExclScorer.java
index 64962ebd6e4..bf7defebae2 100644
-- a/lucene/core/src/java/org/apache/lucene/search/ReqExclScorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/ReqExclScorer.java
@@ -18,7 +18,8 @@ package org.apache.lucene.search;
  */
 
 import java.io.IOException;

import java.util.Collection;
import java.util.Collections;
 
 /** A Scorer for queries with a required subscorer
  * and an excluding (prohibited) sub DocIdSetIterator.
@@ -103,6 +104,16 @@ class ReqExclScorer extends Scorer {
     return reqScorer.score(); // reqScorer may be null when next() or skipTo() already return false
   }
   
  @Override
  public float freq() throws IOException {
    return reqScorer.freq();
  }

  @Override
  public Collection<ChildScorer> getChildren() {
    return Collections.singleton(new ChildScorer(reqScorer, "FILTERED"));
  }

   @Override
   public int advance(int target) throws IOException {
     if (reqScorer == null) {
diff --git a/lucene/core/src/java/org/apache/lucene/search/ReqOptSumScorer.java b/lucene/core/src/java/org/apache/lucene/search/ReqOptSumScorer.java
index 1861b34a52c..d9839634911 100644
-- a/lucene/core/src/java/org/apache/lucene/search/ReqOptSumScorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/ReqOptSumScorer.java
@@ -17,6 +17,8 @@ package org.apache.lucene.search;
  */
 
 import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
 
 /** A Scorer for queries with a required part and an optional part.
  * Delays skipTo() on the optional part until a score() is needed.
@@ -39,6 +41,8 @@ class ReqOptSumScorer extends Scorer {
       Scorer optScorer)
   {
     super(reqScorer.weight);
    assert reqScorer != null;
    assert optScorer != null;
     this.reqScorer = reqScorer;
     this.optScorer = optScorer;
   }
@@ -80,5 +84,19 @@ class ReqOptSumScorer extends Scorer {
     return optScorerDoc == curDoc ? reqScore + optScorer.score() : reqScore;
   }
 
  @Override
  public float freq() throws IOException {
    // we might have deferred advance()
    score();
    return (optScorer != null && optScorer.docID() == reqScorer.docID()) ? 2 : 1;
  }

  @Override
  public Collection<ChildScorer> getChildren() {
    ArrayList<ChildScorer> children = new ArrayList<ChildScorer>(2);
    children.add(new ChildScorer(reqScorer, "MUST"));
    children.add(new ChildScorer(optScorer, "SHOULD"));
    return children;
  }
 }
 
diff --git a/lucene/core/src/java/org/apache/lucene/search/ScoreCachingWrappingScorer.java b/lucene/core/src/java/org/apache/lucene/search/ScoreCachingWrappingScorer.java
index 1a005dc358c..cabadf507b3 100644
-- a/lucene/core/src/java/org/apache/lucene/search/ScoreCachingWrappingScorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/ScoreCachingWrappingScorer.java
@@ -18,6 +18,8 @@ package org.apache.lucene.search;
  */
 
 import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
 
 /**
  * A {@link Scorer} which wraps another scorer and caches the score of the
@@ -58,6 +60,11 @@ public class ScoreCachingWrappingScorer extends Scorer {
     return curScore;
   }
 
  @Override
  public float freq() throws IOException {
    return scorer.freq();
  }

   @Override
   public int docID() {
     return scorer.docID();
@@ -77,5 +84,9 @@ public class ScoreCachingWrappingScorer extends Scorer {
   public int advance(int target) throws IOException {
     return scorer.advance(target);
   }
  

  @Override
  public Collection<ChildScorer> getChildren() {
    return Collections.singleton(new ChildScorer(scorer, "CACHED"));
  }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/search/Scorer.java b/lucene/core/src/java/org/apache/lucene/search/Scorer.java
index f1c67e1c8ef..f654d74e5fb 100644
-- a/lucene/core/src/java/org/apache/lucene/search/Scorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/Scorer.java
@@ -98,9 +98,7 @@ public abstract class Scorer extends DocIdSetIterator {
    *  "sloppy" the match was.
    *
    * @lucene.experimental */
  public float freq() throws IOException {
    throw new UnsupportedOperationException(this + " does not implement freq()");
  }
  public abstract float freq() throws IOException;
   
   /** returns parent Weight
    * @lucene.experimental
diff --git a/lucene/core/src/java/org/apache/lucene/search/TermQuery.java b/lucene/core/src/java/org/apache/lucene/search/TermQuery.java
index 7d781f2eb29..a9a55bc1424 100644
-- a/lucene/core/src/java/org/apache/lucene/search/TermQuery.java
++ b/lucene/core/src/java/org/apache/lucene/search/TermQuery.java
@@ -85,27 +85,20 @@ public class TermQuery extends Query {
       }
       DocsEnum docs = termsEnum.docs(acceptDocs, null, true);
       if (docs != null) {
        return new TermScorer(this, docs, createDocScorer(context));
        return new TermScorer(this, docs, similarity.exactSimScorer(stats, context), termsEnum.docFreq());
       } else {
         // Index does not store freq info
         docs = termsEnum.docs(acceptDocs, null, false);
         assert docs != null;
        return new MatchOnlyTermScorer(this, docs, createDocScorer(context));
        return new MatchOnlyTermScorer(this, docs, similarity.exactSimScorer(stats, context), termsEnum.docFreq());
       }
     }
     
    /**
     * Creates an {@link ExactSimScorer} for this {@link TermWeight}*/
    ExactSimScorer createDocScorer(AtomicReaderContext context)
        throws IOException {
      return similarity.exactSimScorer(stats, context);
    }
    
     /**
      * Returns a {@link TermsEnum} positioned at this weights Term or null if
      * the term does not exist in the given context
      */
    TermsEnum getTermsEnum(AtomicReaderContext context) throws IOException {
    private TermsEnum getTermsEnum(AtomicReaderContext context) throws IOException {
       final TermState state = termStates.get(context.ord);
       if (state == null) { // term is not present in that reader
         assert termNotInReader(context.reader(), term.field(), term.bytes()) : "no termstate found but term exists in reader term=" + term;
diff --git a/lucene/core/src/java/org/apache/lucene/search/TermScorer.java b/lucene/core/src/java/org/apache/lucene/search/TermScorer.java
index ebdee4b9600..3aff7f15895 100644
-- a/lucene/core/src/java/org/apache/lucene/search/TermScorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/TermScorer.java
@@ -27,6 +27,7 @@ import org.apache.lucene.search.similarities.Similarity;
 final class TermScorer extends Scorer {
   private final DocsEnum docsEnum;
   private final Similarity.ExactSimScorer docScorer;
  private final int docFreq;
   
   /**
    * Construct a <code>TermScorer</code>.
@@ -38,11 +39,14 @@ final class TermScorer extends Scorer {
    * @param docScorer
    *          The </code>Similarity.ExactSimScorer</code> implementation 
    *          to be used for score computations.
   * @param docFreq
   *          per-segment docFreq of this term
    */
  TermScorer(Weight weight, DocsEnum td, Similarity.ExactSimScorer docScorer) {
  TermScorer(Weight weight, DocsEnum td, Similarity.ExactSimScorer docScorer, int docFreq) {
     super(weight);
     this.docScorer = docScorer;
     this.docsEnum = td;
    this.docFreq = docFreq;
   }
 
   @Override
@@ -89,4 +93,17 @@ final class TermScorer extends Scorer {
   @Override
   public String toString() { return "scorer(" + weight + ")"; }
 
  // TODO: benchmark if the specialized conjunction really benefits
  // from this, or if instead its from sorting by docFreq, or both

  DocsEnum getDocsEnum() {
    return docsEnum;
  }
  
  // TODO: generalize something like this for scorers?
  // even this is just an estimation...
  
  int getDocFreq() {
    return docFreq;
  }
 }
diff --git a/lucene/core/src/java/org/apache/lucene/util/ScorerDocQueue.java b/lucene/core/src/java/org/apache/lucene/util/ScorerDocQueue.java
deleted file mode 100755
index c53aa0b828c..00000000000
-- a/lucene/core/src/java/org/apache/lucene/util/ScorerDocQueue.java
++ /dev/null
@@ -1,219 +0,0 @@
package org.apache.lucene.util;

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

/* Derived from org.apache.lucene.util.PriorityQueue of March 2005 */

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;

/** A ScorerDocQueue maintains a partial ordering of its Scorers such that the
  least Scorer can always be found in constant time.  Put()'s and pop()'s
  require log(size) time. The ordering is by Scorer.doc().
 *
 * @lucene.internal
 */
public class ScorerDocQueue {  // later: SpansQueue for spans with doc and term positions
  private final HeapedScorerDoc[] heap;
  private final int maxSize;
  private int size;
  
  private class HeapedScorerDoc {
    Scorer scorer;
    int doc;
    
    HeapedScorerDoc(Scorer s) { this(s, s.docID()); }
    
    HeapedScorerDoc(Scorer scorer, int doc) {
      this.scorer = scorer;
      this.doc = doc;
    }
    
    void adjust() { doc = scorer.docID(); }
  }
  
  private HeapedScorerDoc topHSD; // same as heap[1], only for speed

  /** Create a ScorerDocQueue with a maximum size. */
  public ScorerDocQueue(int maxSize) {
    // assert maxSize >= 0;
    size = 0;
    int heapSize = maxSize + 1;
    heap = new HeapedScorerDoc[heapSize];
    this.maxSize = maxSize;
    topHSD = heap[1]; // initially null
  }

  /**
   * Adds a Scorer to a ScorerDocQueue in log(size) time.
   * If one tries to add more Scorers than maxSize
   * a RuntimeException (ArrayIndexOutOfBound) is thrown.
   */
  public final void put(Scorer scorer) {
    size++;
    heap[size] = new HeapedScorerDoc(scorer);
    upHeap();
  }

  /**
   * Adds a Scorer to the ScorerDocQueue in log(size) time if either
   * the ScorerDocQueue is not full, or not lessThan(scorer, top()).
   * @param scorer
   * @return true if scorer is added, false otherwise.
   */
  public boolean insert(Scorer scorer){
    if (size < maxSize) {
      put(scorer);
      return true;
    } else {
      int docNr = scorer.docID();
      if ((size > 0) && (! (docNr < topHSD.doc))) { // heap[1] is top()
        heap[1] = new HeapedScorerDoc(scorer, docNr);
        downHeap();
        return true;
      } else {
        return false;
      }
    }
   }

  /** Returns the least Scorer of the ScorerDocQueue in constant time.
   * Should not be used when the queue is empty.
   */
  public final Scorer top() {
    // assert size > 0;
    return topHSD.scorer;
  }

  /** Returns document number of the least Scorer of the ScorerDocQueue
   * in constant time.
   * Should not be used when the queue is empty.
   */
  public final int topDoc() {
    // assert size > 0;
    return topHSD.doc;
  }
  
  public final float topScore() throws IOException {
    // assert size > 0;
    return topHSD.scorer.score();
  }

  public final boolean topNextAndAdjustElsePop() throws IOException {
    return checkAdjustElsePop(topHSD.scorer.nextDoc() != DocIdSetIterator.NO_MORE_DOCS);
  }

  public final boolean topSkipToAndAdjustElsePop(int target) throws IOException {
    return checkAdjustElsePop(topHSD.scorer.advance(target) != DocIdSetIterator.NO_MORE_DOCS);
  }
  
  private boolean checkAdjustElsePop(boolean cond) {
    if (cond) { // see also adjustTop
      topHSD.doc = topHSD.scorer.docID();
    } else { // see also popNoResult
      heap[1] = heap[size]; // move last to first
      heap[size] = null;
      size--;
    }
    downHeap();
    return cond;
  }

  /** Removes and returns the least scorer of the ScorerDocQueue in log(size)
   * time.
   * Should not be used when the queue is empty.
   */
  public final Scorer pop() {
    // assert size > 0;
    Scorer result = topHSD.scorer;
    popNoResult();
    return result;
  }
  
  /** Removes the least scorer of the ScorerDocQueue in log(size) time.
   * Should not be used when the queue is empty.
   */
  private final void popNoResult() {
    heap[1] = heap[size]; // move last to first
    heap[size] = null;
    size--;
    downHeap();	// adjust heap
  }

  /** Should be called when the scorer at top changes doc() value.
   * Still log(n) worst case, but it's at least twice as fast to <pre>
   *  { pq.top().change(); pq.adjustTop(); }
   * </pre> instead of <pre>
   *  { o = pq.pop(); o.change(); pq.push(o); }
   * </pre>
   */
  public final void adjustTop() {
    // assert size > 0;
    topHSD.adjust();
    downHeap();
  }

  /** Returns the number of scorers currently stored in the ScorerDocQueue. */
  public final int size() {
    return size;
  }

  /** Removes all entries from the ScorerDocQueue. */
  public final void clear() {
    for (int i = 0; i <= size; i++) {
      heap[i] = null;
    }
    size = 0;
  }

  private final void upHeap() {
    int i = size;
    HeapedScorerDoc node = heap[i];		  // save bottom node
    int j = i >>> 1;
    while ((j > 0) && (node.doc < heap[j].doc)) {
      heap[i] = heap[j];			  // shift parents down
      i = j;
      j = j >>> 1;
    }
    heap[i] = node;				  // install saved node
    topHSD = heap[1];
  }

  private final void downHeap() {
    int i = 1;
    HeapedScorerDoc node = heap[i];	          // save top node
    int j = i << 1;				  // find smaller child
    int k = j + 1;
    if ((k <= size) && (heap[k].doc < heap[j].doc)) {
      j = k;
    }
    while ((j <= size) && (heap[j].doc < node.doc)) {
      heap[i] = heap[j];			  // shift up child
      i = j;
      j = i << 1;
      k = j + 1;
      if (k <= size && (heap[k].doc < heap[j].doc)) {
	j = k;
      }
    }
    heap[i] = node;				  // install saved node
    topHSD = heap[1];
  }
}
diff --git a/lucene/core/src/test/org/apache/lucene/search/JustCompileSearch.java b/lucene/core/src/test/org/apache/lucene/search/JustCompileSearch.java
index d631892d34e..b90790b4274 100644
-- a/lucene/core/src/test/org/apache/lucene/search/JustCompileSearch.java
++ b/lucene/core/src/test/org/apache/lucene/search/JustCompileSearch.java
@@ -225,6 +225,11 @@ final class JustCompileSearch {
     public float score() {
       throw new UnsupportedOperationException(UNSUPPORTED_MSG);
     }
    
    @Override
    public float freq() {
      throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }
 
     @Override
     public int docID() {
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestBooleanQueryVisitSubscorers.java b/lucene/core/src/test/org/apache/lucene/search/TestBooleanQueryVisitSubscorers.java
new file mode 100644
index 00000000000..18680633032
-- /dev/null
++ b/lucene/core/src/test/org/apache/lucene/search/TestBooleanQueryVisitSubscorers.java
@@ -0,0 +1,184 @@
package org.apache.lucene.search;

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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Scorer.ChildScorer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;

// TODO: refactor to a base class, that collects freqs from the scorer tree
// and test all queries with it
public class TestBooleanQueryVisitSubscorers extends LuceneTestCase {
  Analyzer analyzer;
  IndexReader reader;
  IndexSearcher searcher;
  Directory dir;
  
  static final String F1 = "title";
  static final String F2 = "body";
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    analyzer = new MockAnalyzer(random());
    dir = newDirectory();
    IndexWriterConfig config = newIndexWriterConfig(TEST_VERSION_CURRENT, analyzer);
    config.setMergePolicy(newLogMergePolicy()); // we will use docids to validate
    RandomIndexWriter writer = new RandomIndexWriter(random(), dir, config);
    writer.addDocument(doc("lucene", "lucene is a very popular search engine library"));
    writer.addDocument(doc("solr", "solr is a very popular search server and is using lucene"));
    writer.addDocument(doc("nutch", "nutch is an internet search engine with web crawler and is using lucene and hadoop"));
    reader = writer.getReader();
    writer.close();
    searcher = new IndexSearcher(reader);
  }
  
  @Override
  public void tearDown() throws Exception {
    reader.close();
    dir.close();
    super.tearDown();
  }

  public void testDisjunctions() throws IOException {
    BooleanQuery bq = new BooleanQuery();
    bq.add(new TermQuery(new Term(F1, "lucene")), BooleanClause.Occur.SHOULD);
    bq.add(new TermQuery(new Term(F2, "lucene")), BooleanClause.Occur.SHOULD);
    bq.add(new TermQuery(new Term(F2, "search")), BooleanClause.Occur.SHOULD);
    Map<Integer,Integer> tfs = getDocCounts(searcher, bq);
    assertEquals(3, tfs.size()); // 3 documents
    assertEquals(3, tfs.get(0).intValue()); // f1:lucene + f2:lucene + f2:search
    assertEquals(2, tfs.get(1).intValue()); // f2:search + f2:lucene
    assertEquals(2, tfs.get(2).intValue()); // f2:search + f2:lucene
  }
  
  public void testNestedDisjunctions() throws IOException {
    BooleanQuery bq = new BooleanQuery();
    bq.add(new TermQuery(new Term(F1, "lucene")), BooleanClause.Occur.SHOULD);
    BooleanQuery bq2 = new BooleanQuery();
    bq2.add(new TermQuery(new Term(F2, "lucene")), BooleanClause.Occur.SHOULD);
    bq2.add(new TermQuery(new Term(F2, "search")), BooleanClause.Occur.SHOULD);
    bq.add(bq2, BooleanClause.Occur.SHOULD);
    Map<Integer,Integer> tfs = getDocCounts(searcher, bq);
    assertEquals(3, tfs.size()); // 3 documents
    assertEquals(3, tfs.get(0).intValue()); // f1:lucene + f2:lucene + f2:search
    assertEquals(2, tfs.get(1).intValue()); // f2:search + f2:lucene
    assertEquals(2, tfs.get(2).intValue()); // f2:search + f2:lucene
  }
  
  public void testConjunctions() throws IOException {
    BooleanQuery bq = new BooleanQuery();
    bq.add(new TermQuery(new Term(F2, "lucene")), BooleanClause.Occur.MUST);
    bq.add(new TermQuery(new Term(F2, "is")), BooleanClause.Occur.MUST);
    Map<Integer,Integer> tfs = getDocCounts(searcher, bq);
    assertEquals(3, tfs.size()); // 3 documents
    assertEquals(2, tfs.get(0).intValue()); // f2:lucene + f2:is
    assertEquals(3, tfs.get(1).intValue()); // f2:is + f2:is + f2:lucene
    assertEquals(3, tfs.get(2).intValue()); // f2:is + f2:is + f2:lucene
  }
  
  static Document doc(String v1, String v2) {
    Document doc = new Document();
    doc.add(new TextField(F1, v1, Store.YES));
    doc.add(new TextField(F2, v2, Store.YES));
    return doc;
  }
  
  static Map<Integer,Integer> getDocCounts(IndexSearcher searcher, Query query) throws IOException {
    MyCollector collector = new MyCollector();
    searcher.search(query, collector);
    return collector.docCounts;
  }
  
  static class MyCollector extends Collector {
    
    private TopDocsCollector<ScoreDoc> collector;
    private int docBase;

    public final Map<Integer,Integer> docCounts = new HashMap<Integer,Integer>();
    private final Set<Scorer> tqsSet = new HashSet<Scorer>();
    
    MyCollector() {
      collector = TopScoreDocCollector.create(10, true);
    }

    @Override
    public boolean acceptsDocsOutOfOrder() {
      return false;
    }

    @Override
    public void collect(int doc) throws IOException {
      int freq = 0;
      for(Scorer scorer : tqsSet) {
        if (doc == scorer.docID()) {
          freq += scorer.freq();
        }
      }
      docCounts.put(doc + docBase, freq);
      collector.collect(doc);
    }

    @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
      this.docBase = context.docBase;
      collector.setNextReader(context);
    }

    @Override
    public void setScorer(Scorer scorer) throws IOException {
      collector.setScorer(scorer);
      tqsSet.clear();
      fillLeaves(scorer, tqsSet);
    }
    
    private void fillLeaves(Scorer scorer, Set<Scorer> set) {
      if (scorer.getWeight().getQuery() instanceof TermQuery) {
        set.add(scorer);
      } else {
        for (ChildScorer child : scorer.getChildren()) {
          fillLeaves(child.child, set);
        }
      }
    }
    
    public TopDocs topDocs(){
      return collector.topDocs();
    }
    
    public int freq(int doc) throws IOException {
      return docCounts.get(doc);
    }
  }
}
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestBooleanScorer.java b/lucene/core/src/test/org/apache/lucene/search/TestBooleanScorer.java
index 1278cd79e0f..4bc0fc7ee2c 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestBooleanScorer.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestBooleanScorer.java
@@ -81,6 +81,7 @@ public class TestBooleanScorer extends LuceneTestCase
     Scorer[] scorers = new Scorer[] {new Scorer(weight) {
       private int doc = -1;
       @Override public float score() { return 0; }
      @Override public float freq()  { return 0; }
       @Override public int docID() { return doc; }
       
       @Override public int nextDoc() {
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestCachingCollector.java b/lucene/core/src/test/org/apache/lucene/search/TestCachingCollector.java
index bbe2bc3da2b..298698b1a82 100755
-- a/lucene/core/src/test/org/apache/lucene/search/TestCachingCollector.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestCachingCollector.java
@@ -34,6 +34,9 @@ public class TestCachingCollector extends LuceneTestCase {
     
     @Override
     public float score() throws IOException { return 0; }
    
    @Override
    public float freq() throws IOException { return 0; }
 
     @Override
     public int docID() { return 0; }
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestConjunctions.java b/lucene/core/src/test/org/apache/lucene/search/TestConjunctions.java
new file mode 100644
index 00000000000..698d0cde240
-- /dev/null
++ b/lucene/core/src/test/org/apache/lucene/search/TestConjunctions.java
@@ -0,0 +1,142 @@
package org.apache.lucene.search;

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

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Norm;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.LuceneTestCase;

public class TestConjunctions extends LuceneTestCase {
  Analyzer analyzer;
  Directory dir;
  IndexReader reader;
  IndexSearcher searcher;
  
  static final String F1 = "title";
  static final String F2 = "body";
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    analyzer = new MockAnalyzer(random());
    dir = newDirectory();
    IndexWriterConfig config = newIndexWriterConfig(TEST_VERSION_CURRENT, analyzer);
    config.setMergePolicy(newLogMergePolicy()); // we will use docids to validate
    RandomIndexWriter writer = new RandomIndexWriter(random(), dir, config);
    writer.addDocument(doc("lucene", "lucene is a very popular search engine library"));
    writer.addDocument(doc("solr", "solr is a very popular search server and is using lucene"));
    writer.addDocument(doc("nutch", "nutch is an internet search engine with web crawler and is using lucene and hadoop"));
    reader = writer.getReader();
    writer.close();
    searcher = new IndexSearcher(reader);
    searcher.setSimilarity(new TFSimilarity());
  }
  
  static Document doc(String v1, String v2) {
    Document doc = new Document();
    doc.add(new StringField(F1, v1, Store.YES));
    doc.add(new TextField(F2, v2, Store.YES));
    return doc;
  }
  
  public void testTermConjunctionsWithOmitTF() throws Exception {
    BooleanQuery bq = new BooleanQuery();
    bq.add(new TermQuery(new Term(F1, "nutch")), BooleanClause.Occur.MUST);
    bq.add(new TermQuery(new Term(F2, "is")), BooleanClause.Occur.MUST);
    TopDocs td = searcher.search(bq, 3);
    assertEquals(1, td.totalHits);
    assertEquals(3F, td.scoreDocs[0].score, 0.001F); // f1:nutch + f2:is + f2:is
  }
  
  @Override
  public void tearDown() throws Exception {
    reader.close();
    dir.close();
    super.tearDown();
  }
  
  // Similarity that returns the TF as score
  private static class TFSimilarity extends Similarity {

    @Override
    public void computeNorm(FieldInvertState state, Norm norm) {
      norm.setByte((byte)1); // we dont care
    }

    @Override
    public SimWeight computeWeight(float queryBoost,
        CollectionStatistics collectionStats, TermStatistics... termStats) {
      return new SimWeight() {
        @Override
        public float getValueForNormalization() {
          return 1; // we don't care
        }
        @Override
        public void normalize(float queryNorm, float topLevelBoost) {
          // we don't care
        }
      };
    }

    @Override
    public ExactSimScorer exactSimScorer(SimWeight weight, AtomicReaderContext context) throws IOException {
      return new ExactSimScorer() {
        @Override
        public float score(int doc, int freq) {
          return freq;
        }
      };
    }

    @Override
    public SloppySimScorer sloppySimScorer(SimWeight weight, AtomicReaderContext context) throws IOException {
      return new SloppySimScorer() {
        @Override
        public float score(int doc, float freq) {
          return freq;
        }
        
        @Override
        public float computeSlopFactor(int distance) {
          return 1F;
        }

        @Override
        public float computePayloadFactor(int doc, int start, int end, BytesRef payload) {
          return 1F;
        }
      };
    }
  }
}
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestPositiveScoresOnlyCollector.java b/lucene/core/src/test/org/apache/lucene/search/TestPositiveScoresOnlyCollector.java
index 988b671d42b..ed9334f28e1 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestPositiveScoresOnlyCollector.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestPositiveScoresOnlyCollector.java
@@ -35,6 +35,10 @@ public class TestPositiveScoresOnlyCollector extends LuceneTestCase {
     @Override public float score() {
       return idx == scores.length ? Float.NaN : scores[idx];
     }
    
    @Override public float freq() {
      return 1;
    }
 
     @Override public int docID() { return idx; }
 
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestScoreCachingWrappingScorer.java b/lucene/core/src/test/org/apache/lucene/search/TestScoreCachingWrappingScorer.java
index 52c68d99abc..dc52313923f 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestScoreCachingWrappingScorer.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestScoreCachingWrappingScorer.java
@@ -43,6 +43,10 @@ public class TestScoreCachingWrappingScorer extends LuceneTestCase {
       // once per document.
       return idx == scores.length ? Float.NaN : scores[idx++];
     }
    
    @Override public float freq() throws IOException {
      return 1;
    }
 
     @Override public int docID() { return doc; }
 
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestSubScorerFreqs.java b/lucene/core/src/test/org/apache/lucene/search/TestSubScorerFreqs.java
index f3b368e16e8..3a593b4e38c 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestSubScorerFreqs.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestSubScorerFreqs.java
@@ -75,7 +75,7 @@ public class TestSubScorerFreqs extends LuceneTestCase {
     private final Set<String> relationships;
 
     public CountingCollector(Collector other) {
      this(other, new HashSet<String>(Arrays.asList(Occur.MUST.toString(), Occur.SHOULD.toString(), Occur.MUST_NOT.toString())));
      this(other, new HashSet<String>(Arrays.asList("MUST", "SHOULD", "MUST_NOT")));
     }
 
     public CountingCollector(Collector other, Set<String> relationships) {
@@ -161,9 +161,9 @@ public class TestSubScorerFreqs extends LuceneTestCase {
     query.add(inner, Occur.MUST);
     query.add(aQuery, Occur.MUST);
     query.add(dQuery, Occur.MUST);
    @SuppressWarnings({"rawtypes","unchecked"}) Set<String>[] occurList = new Set[] {
        Collections.singleton(Occur.MUST.toString()), 
        new HashSet<String>(Arrays.asList(Occur.MUST.toString(), Occur.SHOULD.toString()))
    Set<String>[] occurList = new Set[] {
        Collections.singleton("MUST"), 
        new HashSet<String>(Arrays.asList("MUST", "SHOULD"))
     };
     for (Set<String> occur : occurList) {
       CountingCollector c = new CountingCollector(TopScoreDocCollector.create(
@@ -171,7 +171,7 @@ public class TestSubScorerFreqs extends LuceneTestCase {
       s.search(query, null, c);
       final int maxDocs = s.getIndexReader().maxDoc();
       assertEquals(maxDocs, c.docCounts.size());
      boolean includeOptional = occur.contains(Occur.SHOULD.toString());
      boolean includeOptional = occur.contains("SHOULD");
       for (int i = 0; i < maxDocs; i++) {
         Map<Query, Float> doc0 = c.docCounts.get(i);
         assertEquals(includeOptional ? 5 : 4, doc0.size());
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/BlockGroupingCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/BlockGroupingCollector.java
index 36787689aad..22e0b540e31 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/BlockGroupingCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/BlockGroupingCollector.java
@@ -97,6 +97,11 @@ public class BlockGroupingCollector extends Collector {
     public float score() {
       return score;
     }
    
    @Override
    public float freq() {
      throw new UnsupportedOperationException(); // TODO: wtf does this class do?
    }
 
     @Override
     public int docID() {
diff --git a/lucene/join/src/java/org/apache/lucene/search/join/TermsIncludingScoreQuery.java b/lucene/join/src/java/org/apache/lucene/search/join/TermsIncludingScoreQuery.java
index dd0ea9dde81..4f01d1df3b6 100644
-- a/lucene/join/src/java/org/apache/lucene/search/join/TermsIncludingScoreQuery.java
++ b/lucene/join/src/java/org/apache/lucene/search/join/TermsIncludingScoreQuery.java
@@ -208,6 +208,11 @@ class TermsIncludingScoreQuery extends Query {
       } while (docId != DocIdSetIterator.NO_MORE_DOCS);
       return docId;
     }

    @Override
    public float freq() {
      return 1;
    }
   }
 
   // This impl that tracks whether a docid has already been emitted. This check makes sure that docs aren't emitted
diff --git a/lucene/join/src/java/org/apache/lucene/search/join/ToChildBlockJoinQuery.java b/lucene/join/src/java/org/apache/lucene/search/join/ToChildBlockJoinQuery.java
index 3d2e56ed5d2..55a609e0d01 100644
-- a/lucene/join/src/java/org/apache/lucene/search/join/ToChildBlockJoinQuery.java
++ b/lucene/join/src/java/org/apache/lucene/search/join/ToChildBlockJoinQuery.java
@@ -161,6 +161,7 @@ public class ToChildBlockJoinQuery extends Query {
     private final Bits acceptDocs;
 
     private float parentScore;
    private float parentFreq = 1;
 
     private int childDoc = -1;
     private int parentDoc;
@@ -175,7 +176,7 @@ public class ToChildBlockJoinQuery extends Query {
 
     @Override
     public Collection<ChildScorer> getChildren() {
      return Collections.singletonList(new ChildScorer(parentScorer, "BLOCK_JOIN"));
      return Collections.singleton(new ChildScorer(parentScorer, "BLOCK_JOIN"));
     }
 
     @Override
@@ -218,6 +219,7 @@ public class ToChildBlockJoinQuery extends Query {
             if (childDoc < parentDoc) {
               if (doScores) {
                 parentScore = parentScorer.score();
                parentFreq = parentScorer.freq();
               }
               //System.out.println("  " + childDoc);
               return childDoc;
@@ -247,6 +249,11 @@ public class ToChildBlockJoinQuery extends Query {
       return parentScore;
     }
 
    @Override
    public float freq() throws IOException {
      return parentFreq;
    }

     @Override
     public int advance(int childTarget) throws IOException {
       assert childTarget >= parentBits.length() || !parentBits.get(childTarget);
@@ -269,6 +276,7 @@ public class ToChildBlockJoinQuery extends Query {
         }
         if (doScores) {
           parentScore = parentScorer.score();
          parentFreq = parentScorer.freq();
         }
         final int firstChild = parentBits.prevSetBit(parentDoc-1);
         //System.out.println("  firstChild=" + firstChild);
diff --git a/lucene/join/src/java/org/apache/lucene/search/join/ToParentBlockJoinCollector.java b/lucene/join/src/java/org/apache/lucene/search/join/ToParentBlockJoinCollector.java
index 054e8bdf889..a166376ebfd 100644
-- a/lucene/join/src/java/org/apache/lucene/search/join/ToParentBlockJoinCollector.java
++ b/lucene/join/src/java/org/apache/lucene/search/join/ToParentBlockJoinCollector.java
@@ -327,6 +327,11 @@ public class ToParentBlockJoinCollector extends Collector {
     public float score() {
       return score;
     }
    
    @Override
    public float freq() {
      return 1; // TODO: does anything else make sense?... duplicate of grouping's FakeScorer btw?
    }
 
     @Override
     public int docID() {
diff --git a/lucene/join/src/java/org/apache/lucene/search/join/ToParentBlockJoinQuery.java b/lucene/join/src/java/org/apache/lucene/search/join/ToParentBlockJoinQuery.java
index 7e4c0f13dbf..f113aea67a2 100644
-- a/lucene/join/src/java/org/apache/lucene/search/join/ToParentBlockJoinQuery.java
++ b/lucene/join/src/java/org/apache/lucene/search/join/ToParentBlockJoinQuery.java
@@ -218,6 +218,7 @@ public class ToParentBlockJoinQuery extends Query {
     private int parentDoc = -1;
     private int prevParentDoc;
     private float parentScore;
    private float parentFreq;
     private int nextChildDoc;
 
     private int[] pendingChildDocs = new int[5];
@@ -239,7 +240,7 @@ public class ToParentBlockJoinQuery extends Query {
 
     @Override
     public Collection<ChildScorer> getChildren() {
      return Collections.singletonList(new ChildScorer(childScorer, "BLOCK_JOIN"));
      return Collections.singleton(new ChildScorer(childScorer, "BLOCK_JOIN"));
     }
 
     int getChildCount() {
@@ -299,7 +300,9 @@ public class ToParentBlockJoinQuery extends Query {
         }
 
         float totalScore = 0;
        float totalFreq = 0;
         float maxScore = Float.NEGATIVE_INFINITY;
        float maxFreq = 0;
 
         childDocUpto = 0;
         do {
@@ -315,9 +318,12 @@ public class ToParentBlockJoinQuery extends Query {
           if (scoreMode != ScoreMode.None) {
             // TODO: specialize this into dedicated classes per-scoreMode
             final float childScore = childScorer.score();
            final float childFreq = childScorer.freq();
             pendingChildScores[childDocUpto] = childScore;
             maxScore = Math.max(childScore, maxScore);
            maxFreq = Math.max(childFreq, maxFreq);
             totalScore += childScore;
            totalFreq += childFreq;
           }
           childDocUpto++;
           nextChildDoc = childScorer.nextDoc();
@@ -329,12 +335,15 @@ public class ToParentBlockJoinQuery extends Query {
         switch(scoreMode) {
         case Avg:
           parentScore = totalScore / childDocUpto;
          parentFreq = totalFreq / childDocUpto;
           break;
         case Max:
           parentScore = maxScore;
          parentFreq = maxFreq;
           break;
         case Total:
           parentScore = totalScore;
          parentFreq = totalFreq;
           break;
         case None:
           break;
@@ -354,6 +363,11 @@ public class ToParentBlockJoinQuery extends Query {
     public float score() throws IOException {
       return parentScore;
     }
    
    @Override
    public float freq() {
      return parentFreq;
    }
 
     @Override
     public int advance(int parentTarget) throws IOException {
diff --git a/lucene/queries/src/java/org/apache/lucene/queries/CustomScoreQuery.java b/lucene/queries/src/java/org/apache/lucene/queries/CustomScoreQuery.java
index 230b5577f93..7e7a250bef8 100755
-- a/lucene/queries/src/java/org/apache/lucene/queries/CustomScoreQuery.java
++ b/lucene/queries/src/java/org/apache/lucene/queries/CustomScoreQuery.java
@@ -18,6 +18,8 @@ package org.apache.lucene.queries;
  */
 
 import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
 import java.util.Set;
 import java.util.Arrays;
 
@@ -324,6 +326,16 @@ public class CustomScoreQuery extends Query {
       return qWeight * provider.customScore(subQueryScorer.docID(), subQueryScorer.score(), vScores);
     }
 
    @Override
    public float freq() throws IOException {
      return subQueryScorer.freq();
    }

    @Override
    public Collection<ChildScorer> getChildren() {
      return Collections.singleton(new ChildScorer(subQueryScorer, "CUSTOM"));
    }

     @Override
     public int advance(int target) throws IOException {
       int doc = subQueryScorer.advance(target);
diff --git a/lucene/queries/src/java/org/apache/lucene/queries/function/BoostedQuery.java b/lucene/queries/src/java/org/apache/lucene/queries/function/BoostedQuery.java
index f2b22caeaa1..b1759126d82 100755
-- a/lucene/queries/src/java/org/apache/lucene/queries/function/BoostedQuery.java
++ b/lucene/queries/src/java/org/apache/lucene/queries/function/BoostedQuery.java
@@ -25,6 +25,8 @@ import org.apache.lucene.util.Bits;
 import org.apache.lucene.util.ToStringUtils;
 
 import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
 import java.util.Set;
 import java.util.Map;
 
@@ -164,6 +166,16 @@ public class BoostedQuery extends Query {
       return score>Float.NEGATIVE_INFINITY ? score : -Float.MAX_VALUE;
     }
 
    @Override
    public float freq() throws IOException {
      return scorer.freq();
    }

    @Override
    public Collection<ChildScorer> getChildren() {
      return Collections.singleton(new ChildScorer(scorer, "CUSTOM"));
    }

     public Explanation explain(int doc) throws IOException {
       Explanation subQueryExpl = weight.qWeight.explain(readerContext ,doc);
       if (!subQueryExpl.isMatch()) {
diff --git a/lucene/queries/src/java/org/apache/lucene/queries/function/FunctionQuery.java b/lucene/queries/src/java/org/apache/lucene/queries/function/FunctionQuery.java
index bba3219dc25..558f8b838d0 100644
-- a/lucene/queries/src/java/org/apache/lucene/queries/function/FunctionQuery.java
++ b/lucene/queries/src/java/org/apache/lucene/queries/function/FunctionQuery.java
@@ -158,6 +158,11 @@ public class FunctionQuery extends Query {
       return score>Float.NEGATIVE_INFINITY ? score : -Float.MAX_VALUE;
     }
 
    @Override
    public float freq() throws IOException {
      return 1;
    }

     public Explanation explain(int doc) throws IOException {
       float sc = qWeight * vals.floatVal(doc);
 
diff --git a/lucene/queries/src/java/org/apache/lucene/queries/function/ValueSourceScorer.java b/lucene/queries/src/java/org/apache/lucene/queries/function/ValueSourceScorer.java
index 0401a950c79..677ecfb65d3 100644
-- a/lucene/queries/src/java/org/apache/lucene/queries/function/ValueSourceScorer.java
++ b/lucene/queries/src/java/org/apache/lucene/queries/function/ValueSourceScorer.java
@@ -82,4 +82,9 @@ public class ValueSourceScorer extends Scorer {
   public float score() throws IOException {
     return values.floatVal(doc);
   }

  @Override
  public float freq() throws IOException {
    return 1;
  }
 }
diff --git a/solr/core/src/java/org/apache/solr/schema/LatLonType.java b/solr/core/src/java/org/apache/solr/schema/LatLonType.java
index 87588cd3eef..32eb48e1701 100644
-- a/solr/core/src/java/org/apache/solr/schema/LatLonType.java
++ b/solr/core/src/java/org/apache/solr/schema/LatLonType.java
@@ -485,6 +485,11 @@ class SpatialDistanceQuery extends ExtendedQueryBase implements PostFilter {
       return (float)(dist * qWeight);
     }
 
    @Override
    public float freq() throws IOException {
      return 1;
    }

     public Explanation explain(int doc) throws IOException {
       advance(doc);
       boolean matched = this.doc == doc;
diff --git a/solr/core/src/java/org/apache/solr/search/JoinQParserPlugin.java b/solr/core/src/java/org/apache/solr/search/JoinQParserPlugin.java
index 110bf026879..53a39db5cb4 100644
-- a/solr/core/src/java/org/apache/solr/search/JoinQParserPlugin.java
++ b/solr/core/src/java/org/apache/solr/search/JoinQParserPlugin.java
@@ -532,6 +532,11 @@ class JoinQuery extends Query {
     public float score() throws IOException {
       return score;
     }
    
    @Override
    public float freq() throws IOException {
      return 1;
    }
 
     @Override
     public int advance(int target) throws IOException {
diff --git a/solr/core/src/java/org/apache/solr/search/SolrConstantScoreQuery.java b/solr/core/src/java/org/apache/solr/search/SolrConstantScoreQuery.java
index 97fcf48a622..05a13157437 100755
-- a/solr/core/src/java/org/apache/solr/search/SolrConstantScoreQuery.java
++ b/solr/core/src/java/org/apache/solr/search/SolrConstantScoreQuery.java
@@ -186,6 +186,11 @@ public class SolrConstantScoreQuery extends ConstantScoreQuery implements Extend
     public float score() throws IOException {
       return theScore;
     }
    
    @Override
    public float freq() throws IOException {
      return 1;
    }
 
     @Override
     public int advance(int target) throws IOException {
- 
2.19.1.windows.1

