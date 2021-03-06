From 85642f59ddf00c469b502f46fbc39944e076190e Mon Sep 17 00:00:00 2001
From: Adrien Grand <jpountz@apache.org>
Date: Fri, 4 Apr 2014 15:31:00 +0000
Subject: [PATCH] LUCENE-5527: Refactor Collector API to use a dedicated
 Collector per leaf.

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1584747 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |   3 +
 lucene/MIGRATE.txt                            |   7 +
 .../apache/lucene/search/BooleanScorer.java   |  15 +-
 .../org/apache/lucene/search/BulkScorer.java  |   4 +-
 .../lucene/search/CachingCollector.java       | 478 ++++++++----------
 .../search/CollectionTerminatedException.java |   2 +-
 .../org/apache/lucene/search/Collector.java   | 121 +----
 .../lucene/search/ConstantScoreQuery.java     |  23 +-
 .../org/apache/lucene/search/FakeScorer.java  |   2 +-
 .../apache/lucene/search/FilterCollector.java |  48 ++
 .../lucene/search/FilterLeafCollector.java    |  56 ++
 .../apache/lucene/search/FilteredQuery.java   |   2 +-
 .../apache/lucene/search/IndexSearcher.java   |  19 +-
 .../apache/lucene/search/LeafCollector.java   | 121 +++++
 .../apache/lucene/search/MultiCollector.java  |  65 ++-
 .../search/PositiveScoresOnlyCollector.java   |  51 +-
 .../search/ScoreCachingWrappingScorer.java    |   4 +-
 .../java/org/apache/lucene/search/Scorer.java |   2 +-
 .../apache/lucene/search/SimpleCollector.java |  53 ++
 .../apache/lucene/search/SortRescorer.java    |   2 +-
 .../lucene/search/TimeLimitingCollector.java  |  55 +-
 .../lucene/search/TopDocsCollector.java       |   2 +-
 .../lucene/search/TopFieldCollector.java      |   6 +-
 .../lucene/search/TopScoreDocCollector.java   |  14 +-
 .../lucene/search/TotalHitCountCollector.java |  11 +-
 .../java/org/apache/lucene/search/Weight.java |   4 +-
 .../org/apache/lucene/search/package.html     |   4 +-
 .../org/apache/lucene/index/TestOmitTf.java   |   8 +-
 .../lucene/search/JustCompileSearch.java      |   8 +-
 .../lucene/search/MultiCollectorTest.java     |  22 +-
 .../apache/lucene/search/TestBooleanOr.java   |   9 +-
 .../TestBooleanQueryVisitSubscorers.java      |  66 +--
 .../lucene/search/TestBooleanScorer.java      |  14 +-
 .../lucene/search/TestCachingCollector.java   |  58 +--
 .../lucene/search/TestConstantScoreQuery.java |   6 +-
 .../apache/lucene/search/TestDocBoost.java    |   4 +-
 .../lucene/search/TestEarlyTermination.java   |   7 +-
 .../search/TestMultiTermConstantScore.java    |   4 +-
 .../TestPositiveScoresOnlyCollector.java      |   7 +-
 .../TestScoreCachingWrappingScorer.java       |   5 +-
 .../apache/lucene/search/TestScorerPerf.java  |   7 +-
 .../apache/lucene/search/TestSimilarity.java  |  17 +-
 .../lucene/search/TestSloppyPhraseQuery.java  |  13 +-
 .../lucene/search/TestSubScorerFreqs.java     |  60 +--
 .../apache/lucene/search/TestTermScorer.java  |   4 +-
 .../search/TestTimeLimitingCollector.java     |   4 +-
 .../lucene/search/TestTopDocsCollector.java   |   2 +-
 .../apache/lucene/facet/DrillSideways.java    |   3 +-
 .../lucene/facet/DrillSidewaysScorer.java     |  45 +-
 .../apache/lucene/facet/FacetsCollector.java  |   7 +-
 .../AssertingSubDocsAtOnceCollector.java      |  11 +-
 .../lucene/facet/TestDrillSideways.java       |  10 +-
 .../AbstractAllGroupHeadsCollector.java       |  10 +-
 .../grouping/AbstractAllGroupsCollector.java  |  10 +-
 .../AbstractDistinctValuesCollector.java      |  14 +-
 .../AbstractFirstPassGroupingCollector.java   |   4 +-
 .../grouping/AbstractGroupFacetCollector.java |   3 +-
 .../AbstractSecondPassGroupingCollector.java  |   6 +-
 .../grouping/BlockGroupingCollector.java      |   6 +-
 .../FunctionAllGroupHeadsCollector.java       |   3 +-
 .../function/FunctionAllGroupsCollector.java  |   3 +-
 .../FunctionDistinctValuesCollector.java      |   3 +-
 .../FunctionFirstPassGroupingCollector.java   |   5 +-
 .../FunctionSecondPassGroupingCollector.java  |   5 +-
 .../term/TermAllGroupHeadsCollector.java      |   9 +-
 .../grouping/term/TermAllGroupsCollector.java |   3 +-
 .../term/TermDistinctValuesCollector.java     |   3 +-
 .../term/TermFirstPassGroupingCollector.java  |   5 +-
 .../term/TermGroupFacetCollector.java         |   5 +-
 .../term/TermSecondPassGroupingCollector.java |   5 +-
 .../highlight/HighlighterPhraseTest.java      |   7 +-
 .../apache/lucene/search/join/FakeScorer.java |   4 +-
 .../lucene/search/join/TermsCollector.java    |  12 +-
 .../search/join/TermsIncludingScoreQuery.java |   3 +-
 .../search/join/TermsWithScoreCollector.java  |   8 +-
 .../join/ToParentBlockJoinCollector.java      |   6 +-
 .../lucene/search/join/TestJoinUtil.java      |  32 +-
 .../lucene/index/memory/MemoryIndex.java      |   5 +-
 .../EarlyTerminatingSortingCollector.java     |  64 ++-
 .../index/sorter/TestEarlyTermination.java    |   8 +-
 .../surround/query/BooleanQueryTst.java       |   8 +-
 .../search/AssertingBulkOutOfOrderScorer.java |   4 +-
 .../lucene/search/AssertingBulkScorer.java    |   8 +-
 .../lucene/search/AssertingCollector.java     |  42 +-
 .../org/apache/lucene/search/CheckHits.java   |   8 +-
 .../org/apache/lucene/search/QueryUtils.java  |   8 +-
 .../accumulator/BasicAccumulator.java         |   2 +-
 .../accumulator/FacetingAccumulator.java      |  10 +-
 .../accumulator/ValueAccumulator.java         |  17 +-
 .../facet/FieldFacetAccumulator.java          |   2 +-
 .../facet/QueryFacetAccumulator.java          |   2 +-
 .../facet/RangeFacetAccumulator.java          |   2 +-
 .../analytics/request/AnalyticsStats.java     |   2 +-
 .../handler/component/ExpandComponent.java    |  75 +--
 .../org/apache/solr/schema/LatLonType.java    |   9 +-
 .../solr/search/CollapsingQParserPlugin.java  |  32 +-
 .../solr/search/DelegatingCollector.java      |  22 +-
 .../apache/solr/search/DocSetCollector.java   |   7 +-
 .../solr/search/DocSetDelegateCollector.java  |  84 ---
 .../search/EarlyTerminatingCollector.java     |  69 +--
 .../solr/search/FunctionRangeQuery.java       |   9 +-
 .../java/org/apache/solr/search/Grouping.java |   6 +-
 .../apache/solr/search/SolrIndexSearcher.java |  65 ++-
 .../solr/search/grouping/CommandHandler.java  |  15 +-
 .../grouping/collector/FilterCollector.java   |  48 +-
 .../test/org/apache/solr/search/TestSort.java |  34 +-
 106 files changed, 1174 insertions(+), 1242 deletions(-)
 create mode 100644 lucene/core/src/java/org/apache/lucene/search/FilterCollector.java
 create mode 100644 lucene/core/src/java/org/apache/lucene/search/FilterLeafCollector.java
 create mode 100644 lucene/core/src/java/org/apache/lucene/search/LeafCollector.java
 create mode 100644 lucene/core/src/java/org/apache/lucene/search/SimpleCollector.java
 delete mode 100644 solr/core/src/java/org/apache/solr/search/DocSetDelegateCollector.java

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 79911beb6c6..a0a266bebb8 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -50,6 +50,9 @@ API Changes
   via setReader.  
   (Benson Margulies via Robert Muir - pull request #16)
 
* LUCENE-5527: The Collector API has been refactored to use a dedicated Collector
  per leaf. (Shikhar Bhushan, Adrien Grand)

 Documentation
 
 * LUCENE-5392: Add/improve analysis package documentation to reflect
diff --git a/lucene/MIGRATE.txt b/lucene/MIGRATE.txt
index 6e26ad6b58d..7cbc53fbc08 100644
-- a/lucene/MIGRATE.txt
++ b/lucene/MIGRATE.txt
@@ -12,3 +12,10 @@ of the return type is enough to upgrade.
 The constructor of Tokenizer no longer takes Reader, as this was a leftover
 from before it was reusable. See the org.apache.lucene.analysis package
 documentation for more details.

## Refactored Collector API (LUCENE-5299)

The Collector API has been refactored to use a different Collector instance
per segment. It is possible to migrate existing collectors painlessly by
extending SimpleCollector instead of Collector: SimpleCollector is a
specialization of Collector that returns itself as a per-segment Collector.
diff --git a/lucene/core/src/java/org/apache/lucene/search/BooleanScorer.java b/lucene/core/src/java/org/apache/lucene/search/BooleanScorer.java
index 7c2b6aa38ca..173bb44760b 100644
-- a/lucene/core/src/java/org/apache/lucene/search/BooleanScorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/BooleanScorer.java
@@ -61,7 +61,7 @@ import org.apache.lucene.search.BooleanQuery.BooleanWeight;
 
 final class BooleanScorer extends BulkScorer {
   
  private static final class BooleanScorerCollector extends Collector {
  private static final class BooleanScorerCollector extends SimpleCollector {
     private BucketTable bucketTable;
     private int mask;
     private Scorer scorer;
@@ -92,11 +92,6 @@ final class BooleanScorer extends BulkScorer {
       }
     }
     
    @Override
    public void setNextReader(AtomicReaderContext context) {
      // not needed by this implementation
    }
    
     @Override
     public void setScorer(Scorer scorer) {
       this.scorer = scorer;
@@ -136,7 +131,7 @@ final class BooleanScorer extends BulkScorer {
       }
     }
 
    public Collector newCollector(int mask) {
    public LeafCollector newCollector(int mask) {
       return new BooleanScorerCollector(mask, this);
     }
 
@@ -148,12 +143,12 @@ final class BooleanScorer extends BulkScorer {
     // TODO: re-enable this if BQ ever sends us required clauses
     //public boolean required = false;
     public boolean prohibited;
    public Collector collector;
    public LeafCollector collector;
     public SubScorer next;
     public boolean more;
 
     public SubScorer(BulkScorer scorer, boolean required, boolean prohibited,
        Collector collector, SubScorer next) {
        LeafCollector collector, SubScorer next) {
       if (required) {
         throw new IllegalArgumentException("this scorer cannot handle required=true");
       }
@@ -200,7 +195,7 @@ final class BooleanScorer extends BulkScorer {
   }
 
   @Override
  public boolean score(Collector collector, int max) throws IOException {
  public boolean score(LeafCollector collector, int max) throws IOException {
 
     boolean more;
     Bucket tmp;
diff --git a/lucene/core/src/java/org/apache/lucene/search/BulkScorer.java b/lucene/core/src/java/org/apache/lucene/search/BulkScorer.java
index 2331cae7f61..7ba1b395ea8 100644
-- a/lucene/core/src/java/org/apache/lucene/search/BulkScorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/BulkScorer.java
@@ -31,7 +31,7 @@ public abstract class BulkScorer {
   /** Scores and collects all matching documents.
    * @param collector The collector to which all matching documents are passed.
    */
  public void score(Collector collector) throws IOException {
  public void score(LeafCollector collector) throws IOException {
     score(collector, Integer.MAX_VALUE);
   }
 
@@ -42,5 +42,5 @@ public abstract class BulkScorer {
    * @param max Score up to, but not including, this doc
    * @return true if more matching documents may remain.
    */
  public abstract boolean score(Collector collector, int max) throws IOException;
  public abstract boolean score(LeafCollector collector, int max) throws IOException;
 }
diff --git a/lucene/core/src/java/org/apache/lucene/search/CachingCollector.java b/lucene/core/src/java/org/apache/lucene/search/CachingCollector.java
index 23e159069ba..c5957d8ead7 100644
-- a/lucene/core/src/java/org/apache/lucene/search/CachingCollector.java
++ b/lucene/core/src/java/org/apache/lucene/search/CachingCollector.java
@@ -18,10 +18,12 @@ package org.apache.lucene.search;
  */
 
 import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.util.ArrayUtil;
 import org.apache.lucene.util.RamUsageEstimator;
 
 import java.io.IOException;
 import java.util.ArrayList;
import java.util.Arrays;
 import java.util.List;
 
 /**
@@ -38,317 +40,280 @@ import java.util.List;
  * scoring is cached) per collected document.  If the result
  * set is large this can easily be a very substantial amount
  * of RAM!
 * 
 * <p><b>NOTE</b>: this class caches at least 128 documents
 * before checking RAM limits.
 * 
 *
  * <p>See the Lucene <tt>modules/grouping</tt> module for more
  * details including a full code example.</p>
  *
  * @lucene.experimental
  */
public abstract class CachingCollector extends Collector {
  
  // Max out at 512K arrays
  private static final int MAX_ARRAY_SIZE = 512 * 1024;
  private static final int INITIAL_ARRAY_SIZE = 128;
  private final static int[] EMPTY_INT_ARRAY = new int[0];
public abstract class CachingCollector extends FilterCollector {
 
  private static class SegStart {
    public final AtomicReaderContext readerContext;
    public final int end;
  private static final int INITIAL_ARRAY_SIZE = 128;
 
    public SegStart(AtomicReaderContext readerContext, int end) {
      this.readerContext = readerContext;
      this.end = end;
    }
  }
  
   private static final class CachedScorer extends Scorer {
    

     // NOTE: these members are package-private b/c that way accessing them from
     // the outer class does not incur access check by the JVM. The same
     // situation would be if they were defined in the outer class as private
     // members.
     int doc;
     float score;
    

     private CachedScorer() { super(null); }
 
     @Override
     public final float score() { return score; }
    

     @Override
     public final int advance(int target) { throw new UnsupportedOperationException(); }
    

     @Override
     public final int docID() { return doc; }
    

     @Override
     public final int freq() { throw new UnsupportedOperationException(); }
    

     @Override
     public final int nextDoc() { throw new UnsupportedOperationException(); }
    

     @Override
     public long cost() { return 1; }
    }

  // A CachingCollector which caches scores
  private static final class ScoreCachingCollector extends CachingCollector {
  }
 
    private final CachedScorer cachedScorer;
    private final List<float[]> cachedScores;
  private static class NoScoreCachingCollector extends CachingCollector {
 
    private Scorer scorer;
    private float[] curScores;
    List<Boolean> acceptDocsOutOfOrders;
    List<AtomicReaderContext> contexts;
    List<int[]> docs;
    int maxDocsToCache;
    NoScoreCachingLeafCollector lastCollector;
 
    ScoreCachingCollector(Collector other, double maxRAMMB) {
      super(other, maxRAMMB, true);
    NoScoreCachingCollector(Collector in, int maxDocsToCache) {
      super(in);
      this.maxDocsToCache = maxDocsToCache;
      contexts = new ArrayList<>();
      acceptDocsOutOfOrders = new ArrayList<>();
      docs = new ArrayList<>();
    }
 
      cachedScorer = new CachedScorer();
      cachedScores = new ArrayList<>();
      curScores = new float[INITIAL_ARRAY_SIZE];
      cachedScores.add(curScores);
    protected NoScoreCachingLeafCollector wrap(LeafCollector in, int maxDocsToCache) {
      return new NoScoreCachingLeafCollector(in, maxDocsToCache);
     }
 
    ScoreCachingCollector(Collector other, int maxDocsToCache) {
      super(other, maxDocsToCache);
    public LeafCollector getLeafCollector(AtomicReaderContext context) throws IOException {
      postCollection();
      final LeafCollector in = this.in.getLeafCollector(context);
      if (contexts != null) {
        contexts.add(context);
        acceptDocsOutOfOrders.add(in.acceptsDocsOutOfOrder());
      }
      if (maxDocsToCache >= 0) {
        return lastCollector = wrap(in, maxDocsToCache);
      } else {
        return in;
      }
    }
 
      cachedScorer = new CachedScorer();
      cachedScores = new ArrayList<>();
      curScores = new float[INITIAL_ARRAY_SIZE];
      cachedScores.add(curScores);
    protected void invalidate() {
      maxDocsToCache = -1;
      contexts = null;
      this.docs = null;
     }
    
    @Override
    public void collect(int doc) throws IOException {
 
      if (curDocs == null) {
        // Cache was too large
        cachedScorer.score = scorer.score();
        cachedScorer.doc = doc;
        other.collect(doc);
        return;
      }
    protected void postCollect(NoScoreCachingLeafCollector collector) {
      final int[] docs = collector.cachedDocs();
      maxDocsToCache -= docs.length;
      this.docs.add(docs);
    }
 
      // Allocate a bigger array or abort caching
      if (upto == curDocs.length) {
        base += upto;
        
        // Compute next array length - don't allocate too big arrays
        int nextLength = 8*curDocs.length;
        if (nextLength > MAX_ARRAY_SIZE) {
          nextLength = MAX_ARRAY_SIZE;
    private void postCollection() {
      if (lastCollector != null) {
        if (!lastCollector.hasCache()) {
          invalidate();
        } else {
          postCollect(lastCollector);
         }
        lastCollector = null;
      }
    }
 
        if (base + nextLength > maxDocsToCache) {
          // try to allocate a smaller array
          nextLength = maxDocsToCache - base;
          if (nextLength <= 0) {
            // Too many docs to collect -- clear cache
            curDocs = null;
            curScores = null;
            cachedSegs.clear();
            cachedDocs.clear();
            cachedScores.clear();
            cachedScorer.score = scorer.score();
            cachedScorer.doc = doc;
            other.collect(doc);
            return;
          }
        }
        
        curDocs = new int[nextLength];
        cachedDocs.add(curDocs);
        curScores = new float[nextLength];
        cachedScores.add(curScores);
        upto = 0;
    protected void collect(LeafCollector collector, int i) throws IOException {
      final int[] docs = this.docs.get(i);
      for (int doc : docs) {
        collector.collect(doc);
       }
      
      curDocs[upto] = doc;
      cachedScorer.score = curScores[upto] = scorer.score();
      upto++;
      cachedScorer.doc = doc;
      other.collect(doc);
     }
 
    @Override
     public void replay(Collector other) throws IOException {
      replayInit(other);
      
      int curUpto = 0;
      int curBase = 0;
      int chunkUpto = 0;
      curDocs = EMPTY_INT_ARRAY;
      for (SegStart seg : cachedSegs) {
        other.setNextReader(seg.readerContext);
        other.setScorer(cachedScorer);
        while (curBase + curUpto < seg.end) {
          if (curUpto == curDocs.length) {
            curBase += curDocs.length;
            curDocs = cachedDocs.get(chunkUpto);
            curScores = cachedScores.get(chunkUpto);
            chunkUpto++;
            curUpto = 0;
          }
          cachedScorer.score = curScores[curUpto];
          cachedScorer.doc = curDocs[curUpto];
          other.collect(curDocs[curUpto++]);
      postCollection();
      if (!isCached()) {
        throw new IllegalStateException("cannot replay: cache was cleared because too much RAM was required");
      }
      assert docs.size() == contexts.size();
      for (int i = 0; i < contexts.size(); ++i) {
        final AtomicReaderContext context = contexts.get(i);
        final boolean docsInOrder = !acceptDocsOutOfOrders.get(i);
        final LeafCollector collector = other.getLeafCollector(context);
        if (!collector.acceptsDocsOutOfOrder() && !docsInOrder) {
          throw new IllegalArgumentException(
                "cannot replay: given collector does not support "
                    + "out-of-order collection, while the wrapped collector does. "
                    + "Therefore cached documents may be out-of-order.");
         }
        collect(collector, i);
       }
     }
 
    @Override
    public void setScorer(Scorer scorer) throws IOException {
      this.scorer = scorer;
      other.setScorer(cachedScorer);
  }

  private static class ScoreCachingCollector extends NoScoreCachingCollector {

    List<float[]> scores;

    ScoreCachingCollector(Collector in, int maxDocsToCache) {
      super(in, maxDocsToCache);
      scores = new ArrayList<>();
    }

    protected NoScoreCachingLeafCollector wrap(LeafCollector in, int maxDocsToCache) {
      return new ScoreCachingLeafCollector(in, maxDocsToCache);
     }
 
     @Override
    public String toString() {
      if (isCached()) {
        return "CachingCollector (" + (base+upto) + " docs & scores cached)";
      } else {
        return "CachingCollector (cache was cleared)";
    protected void postCollect(NoScoreCachingLeafCollector collector) {
      final ScoreCachingLeafCollector coll = (ScoreCachingLeafCollector) collector;
      super.postCollect(coll);
      scores.add(coll.cachedScores());
    }

    protected void collect(LeafCollector collector, int i) throws IOException {
      final int[] docs = this.docs.get(i);
      final float[] scores = this.scores.get(i);
      assert docs.length == scores.length;
      final CachedScorer scorer = new CachedScorer();
      collector.setScorer(scorer);
      for (int j = 0; j < docs.length; ++j) {
        scorer.doc = docs[j];
        scorer.score = scores[j];
        collector.collect(scorer.doc);
       }
     }
 
   }
 
  // A CachingCollector which does not cache scores
  private static final class NoScoreCachingCollector extends CachingCollector {
    
    NoScoreCachingCollector(Collector other, double maxRAMMB) {
     super(other, maxRAMMB, false);
    }
  private class NoScoreCachingLeafCollector extends FilterLeafCollector {
 
    NoScoreCachingCollector(Collector other, int maxDocsToCache) {
     super(other, maxDocsToCache);
    }
    final int maxDocsToCache;
    int[] docs;
    int docCount;
 
    @Override
    public void collect(int doc) throws IOException {
    NoScoreCachingLeafCollector(LeafCollector in, int maxDocsToCache) {
      super(in);
      this.maxDocsToCache = maxDocsToCache;
      docs = new int[Math.min(maxDocsToCache, INITIAL_ARRAY_SIZE)];
      docCount = 0;
    }
 
      if (curDocs == null) {
        // Cache was too large
        other.collect(doc);
        return;
      }
    protected void grow(int newLen) {
      docs = Arrays.copyOf(docs, newLen);
    }
 
      // Allocate a bigger array or abort caching
      if (upto == curDocs.length) {
        base += upto;
        
        // Compute next array length - don't allocate too big arrays
        int nextLength = 8*curDocs.length;
        if (nextLength > MAX_ARRAY_SIZE) {
          nextLength = MAX_ARRAY_SIZE;
        }
    protected void invalidate() {
      docs = null;
      docCount = -1;
      cached = false;
    }
 
        if (base + nextLength > maxDocsToCache) {
          // try to allocate a smaller array
          nextLength = maxDocsToCache - base;
          if (nextLength <= 0) {
            // Too many docs to collect -- clear cache
            curDocs = null;
            cachedSegs.clear();
            cachedDocs.clear();
            other.collect(doc);
            return;
          }
        }
        
        curDocs = new int[nextLength];
        cachedDocs.add(curDocs);
        upto = 0;
      }
      
      curDocs[upto] = doc;
      upto++;
      other.collect(doc);
    protected void buffer(int doc) throws IOException {
      docs[docCount] = doc;
     }
 
     @Override
    public void replay(Collector other) throws IOException {
      replayInit(other);
      
      int curUpto = 0;
      int curbase = 0;
      int chunkUpto = 0;
      curDocs = EMPTY_INT_ARRAY;
      for (SegStart seg : cachedSegs) {
        other.setNextReader(seg.readerContext);
        while (curbase + curUpto < seg.end) {
          if (curUpto == curDocs.length) {
            curbase += curDocs.length;
            curDocs = cachedDocs.get(chunkUpto);
            chunkUpto++;
            curUpto = 0;
    public void collect(int doc) throws IOException {
      if (docs != null) {
        if (docCount >= docs.length) {
          if (docCount >= maxDocsToCache) {
            invalidate();
          } else {
            final int newLen = Math.min(ArrayUtil.oversize(docCount + 1, RamUsageEstimator.NUM_BYTES_INT), maxDocsToCache);
            grow(newLen);
           }
          other.collect(curDocs[curUpto++]);
        }
        if (docs != null) {
          buffer(doc);
          ++docCount;
         }
       }
      super.collect(doc);
    }

    boolean hasCache() {
      return docs != null;
    }

    int[] cachedDocs() {
      return docs == null ? null : Arrays.copyOf(docs, docCount);
    }

  }

  private class ScoreCachingLeafCollector extends NoScoreCachingLeafCollector {

    Scorer scorer;
    float[] scores;

    ScoreCachingLeafCollector(LeafCollector in, int maxDocsToCache) {
      super(in, maxDocsToCache);
      scores = new float[docs.length];
     }
 
     @Override
     public void setScorer(Scorer scorer) throws IOException {
      other.setScorer(scorer);
      this.scorer = scorer;
      super.setScorer(scorer);
     }
 
     @Override
    public String toString() {
      if (isCached()) {
        return "CachingCollector (" + (base+upto) + " docs cached)";
      } else {
        return "CachingCollector (cache was cleared)";
      }
    protected void grow(int newLen) {
      super.grow(newLen);
      scores = Arrays.copyOf(scores, newLen);
    }

    @Override
    protected void invalidate() {
      super.invalidate();
      scores = null;
     }
 
    @Override
    protected void buffer(int doc) throws IOException {
      super.buffer(doc);
      scores[docCount] = scorer.score();
    }

    float[] cachedScores() {
      return docs == null ? null : Arrays.copyOf(scores, docCount);
    }
   }
 
  // TODO: would be nice if a collector defined a
  // needsScores() method so we can specialize / do checks
  // up front. This is only relevant for the ScoreCaching
  // version -- if the wrapped Collector does not need
  // scores, it can avoid cachedScorer entirely.
  protected final Collector other;
  
  protected final int maxDocsToCache;
  protected final List<SegStart> cachedSegs = new ArrayList<>();
  protected final List<int[]> cachedDocs;
  
  private AtomicReaderContext lastReaderContext;
  
  protected int[] curDocs;
  protected int upto;
  protected int base;
  protected int lastDocBase;
  
   /**
    * Creates a {@link CachingCollector} which does not wrap another collector.
    * The cached documents and scores can later be {@link #replay(Collector)
    * replayed}.
   * 
   *
    * @param acceptDocsOutOfOrder
    *          whether documents are allowed to be collected out-of-order
    */
   public static CachingCollector create(final boolean acceptDocsOutOfOrder, boolean cacheScores, double maxRAMMB) {
    Collector other = new Collector() {
    Collector other = new SimpleCollector() {
       @Override
       public boolean acceptsDocsOutOfOrder() {
         return acceptDocsOutOfOrder;
       }
      
      @Override
      public void setScorer(Scorer scorer) {}
 
       @Override
       public void collect(int doc) {}
 
      @Override
      public void setNextReader(AtomicReaderContext context) {}

     };
     return create(other, cacheScores, maxRAMMB);
   }
@@ -356,7 +321,7 @@ public abstract class CachingCollector extends Collector {
   /**
    * Create a new {@link CachingCollector} that wraps the given collector and
    * caches documents and scores up to the specified RAM threshold.
   * 
   *
    * @param other
    *          the Collector to wrap and delegate calls to.
    * @param cacheScores
@@ -368,7 +333,12 @@ public abstract class CachingCollector extends Collector {
    *          scores are cached.
    */
   public static CachingCollector create(Collector other, boolean cacheScores, double maxRAMMB) {
    return cacheScores ? new ScoreCachingCollector(other, maxRAMMB) : new NoScoreCachingCollector(other, maxRAMMB);
    int bytesPerDoc = RamUsageEstimator.NUM_BYTES_INT;
    if (cacheScores) {
      bytesPerDoc += RamUsageEstimator.NUM_BYTES_FLOAT;
    }
    final int maxDocsToCache = (int) ((maxRAMMB * 1024 * 1024) / bytesPerDoc);
    return create(other, cacheScores, maxDocsToCache);
   }
 
   /**
@@ -388,74 +358,26 @@ public abstract class CachingCollector extends Collector {
   public static CachingCollector create(Collector other, boolean cacheScores, int maxDocsToCache) {
     return cacheScores ? new ScoreCachingCollector(other, maxDocsToCache) : new NoScoreCachingCollector(other, maxDocsToCache);
   }
  
  // Prevent extension from non-internal classes
  private CachingCollector(Collector other, double maxRAMMB, boolean cacheScores) {
    this.other = other;
    
    cachedDocs = new ArrayList<>();
    curDocs = new int[INITIAL_ARRAY_SIZE];
    cachedDocs.add(curDocs);

    int bytesPerDoc = RamUsageEstimator.NUM_BYTES_INT;
    if (cacheScores) {
      bytesPerDoc += RamUsageEstimator.NUM_BYTES_FLOAT;
    }
    maxDocsToCache = (int) ((maxRAMMB * 1024 * 1024) / bytesPerDoc);
  }
 
  private CachingCollector(Collector other, int maxDocsToCache) {
    this.other = other;
  private boolean cached;
 
    cachedDocs = new ArrayList<>();
    curDocs = new int[INITIAL_ARRAY_SIZE];
    cachedDocs.add(curDocs);
    this.maxDocsToCache = maxDocsToCache;
  }
  
  @Override
  public boolean acceptsDocsOutOfOrder() {
    return other.acceptsDocsOutOfOrder();
  }

  public boolean isCached() {
    return curDocs != null;
  private CachingCollector(Collector in) {
    super(in);
    cached = true;
   }
 
  @Override  
  public void setNextReader(AtomicReaderContext context) throws IOException {
    other.setNextReader(context);
    if (lastReaderContext != null) {
      cachedSegs.add(new SegStart(lastReaderContext, base+upto));
    }
    lastReaderContext = context;
  }

  /** Reused by the specialized inner classes. */
  void replayInit(Collector other) {
    if (!isCached()) {
      throw new IllegalStateException("cannot replay: cache was cleared because too much RAM was required");
    }
    
    if (!other.acceptsDocsOutOfOrder() && this.other.acceptsDocsOutOfOrder()) {
      throw new IllegalArgumentException(
          "cannot replay: given collector does not support "
              + "out-of-order collection, while the wrapped collector does. "
              + "Therefore cached documents may be out-of-order.");
    }
    
    //System.out.println("CC: replay totHits=" + (upto + base));
    if (lastReaderContext != null) {
      cachedSegs.add(new SegStart(lastReaderContext, base+upto));
      lastReaderContext = null;
    }
  /**
   * Return true is this collector is able to replay collection.
   */
  public final boolean isCached() {
    return cached;
   }
 
   /**
    * Replays the cached doc IDs (and scores) to the given Collector. If this
    * instance does not cache scores, then Scorer is not set on
    * {@code other.setScorer} as well as scores are not replayed.
   * 
   *
    * @throws IllegalStateException
    *           if this collector is not cached (i.e., if the RAM limits were too
    *           low for the number of documents + scores to cache).
@@ -464,5 +386,5 @@ public abstract class CachingCollector extends Collector {
    *           while the collector passed to the ctor does.
    */
   public abstract void replay(Collector other) throws IOException;
  

 }
diff --git a/lucene/core/src/java/org/apache/lucene/search/CollectionTerminatedException.java b/lucene/core/src/java/org/apache/lucene/search/CollectionTerminatedException.java
index 9caadfa2976..a4c426a9d51 100644
-- a/lucene/core/src/java/org/apache/lucene/search/CollectionTerminatedException.java
++ b/lucene/core/src/java/org/apache/lucene/search/CollectionTerminatedException.java
@@ -17,7 +17,7 @@ package org.apache.lucene.search;
  * limitations under the License.
  */
 
/** Throw this exception in {@link Collector#collect(int)} to prematurely
/** Throw this exception in {@link LeafCollector#collect(int)} to prematurely
  *  terminate collection of the current leaf.
  *  <p>Note: IndexSearcher swallows this exception and never re-throws it.
  *  As a consequence, you should not catch it when calling
diff --git a/lucene/core/src/java/org/apache/lucene/search/Collector.java b/lucene/core/src/java/org/apache/lucene/search/Collector.java
index 312f5074b58..bb473946c33 100644
-- a/lucene/core/src/java/org/apache/lucene/search/Collector.java
++ b/lucene/core/src/java/org/apache/lucene/search/Collector.java
@@ -20,20 +20,19 @@ package org.apache.lucene.search;
 import java.io.IOException;
 
 import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReaderContext;
 
 /**
  * <p>Expert: Collectors are primarily meant to be used to
  * gather raw results from a search, and implement sorting
  * or custom result filtering, collation, etc. </p>
  *
 * <p>Lucene's core collectors are derived from Collector.
 * Likely your application can use one of these classes, or
 * subclass {@link TopDocsCollector}, instead of
 * implementing Collector directly:
 * <p>Lucene's core collectors are derived from {@link Collector}
 * and {@link SimpleCollector}. Likely your application can
 * use one of these classes, or subclass {@link TopDocsCollector},
 * instead of implementing Collector directly:
  *
  * <ul>
 *      
 *
  *   <li>{@link TopDocsCollector} is an abstract base class
  *   that assumes you will retrieve the top N docs,
  *   according to some criteria, after collection is
@@ -62,118 +61,16 @@ import org.apache.lucene.index.IndexReaderContext;
  *
  * </ul>
  *
 * <p>Collector decouples the score from the collected doc:
 * the score computation is skipped entirely if it's not
 * needed.  Collectors that do need the score should
 * implement the {@link #setScorer} method, to hold onto the
 * passed {@link Scorer} instance, and call {@link
 * Scorer#score()} within the collect method to compute the
 * current hit's score.  If your collector may request the
 * score for a single hit multiple times, you should use
 * {@link ScoreCachingWrappingScorer}. </p>
 * 
 * <p><b>NOTE:</b> The doc that is passed to the collect
 * method is relative to the current reader. If your
 * collector needs to resolve this to the docID space of the
 * Multi*Reader, you must re-base it by recording the
 * docBase from the most recent setNextReader call.  Here's
 * a simple example showing how to collect docIDs into a
 * BitSet:</p>
 * 
 * <pre class="prettyprint">
 * IndexSearcher searcher = new IndexSearcher(indexReader);
 * final BitSet bits = new BitSet(indexReader.maxDoc());
 * searcher.search(query, new Collector() {
 *   private int docBase;
 * 
 *   <em>// ignore scorer</em>
 *   public void setScorer(Scorer scorer) {
 *   }
 *
 *   <em>// accept docs out of order (for a BitSet it doesn't matter)</em>
 *   public boolean acceptsDocsOutOfOrder() {
 *     return true;
 *   }
 * 
 *   public void collect(int doc) {
 *     bits.set(doc + docBase);
 *   }
 * 
 *   public void setNextReader(AtomicReaderContext context) {
 *     this.docBase = context.docBase;
 *   }
 * });
 * </pre>
 *
 * <p>Not all collectors will need to rebase the docID.  For
 * example, a collector that simply counts the total number
 * of hits would skip it.</p>
 * 
 * <p><b>NOTE:</b> Prior to 2.9, Lucene silently filtered
 * out hits with score <= 0.  As of 2.9, the core Collectors
 * no longer do that.  It's very unusual to have such hits
 * (a negative query boost, or function query returning
 * negative custom scores, could cause it to happen).  If
 * you need that behavior, use {@link
 * PositiveScoresOnlyCollector}.</p>
 *
  * @lucene.experimental
 * 
 * @since 2.9
  */
public abstract class Collector {
  
  /**
   * Called before successive calls to {@link #collect(int)}. Implementations
   * that need the score of the current document (passed-in to
   * {@link #collect(int)}), should save the passed-in Scorer and call
   * scorer.score() when needed.
   */
  public abstract void setScorer(Scorer scorer) throws IOException;
  
  /**
   * Called once for every document matching a query, with the unbased document
   * number.
   * <p>Note: The collection of the current segment can be terminated by throwing
   * a {@link CollectionTerminatedException}. In this case, the last docs of the
   * current {@link AtomicReaderContext} will be skipped and {@link IndexSearcher}
   * will swallow the exception and continue collection with the next leaf.
   * <p>
   * Note: This is called in an inner search loop. For good search performance,
   * implementations of this method should not call {@link IndexSearcher#doc(int)} or
   * {@link org.apache.lucene.index.IndexReader#document(int)} on every hit.
   * Doing so can slow searches by an order of magnitude or more.
   */
  public abstract void collect(int doc) throws IOException;
public interface Collector {
 
   /**
   * Called before collecting from each {@link AtomicReaderContext}. All doc ids in
   * {@link #collect(int)} will correspond to {@link IndexReaderContext#reader}.
   * 
   * Add {@link AtomicReaderContext#docBase} to the current  {@link IndexReaderContext#reader}'s
   * internal document id to re-base ids in {@link #collect(int)}.
   * 
   * Create a new {@link LeafCollector collector} to collect the given context.
   *
    * @param context
    *          next atomic reader context
    */
  public abstract void setNextReader(AtomicReaderContext context) throws IOException;
  LeafCollector getLeafCollector(AtomicReaderContext context) throws IOException;
 
  /**
   * Return <code>true</code> if this collector does not
   * require the matching docIDs to be delivered in int sort
   * order (smallest to largest) to {@link #collect}.
   *
   * <p> Most Lucene Query implementations will visit
   * matching docIDs in order.  However, some queries
   * (currently limited to certain cases of {@link
   * BooleanQuery}) can achieve faster searching if the
   * <code>Collector</code> allows them to deliver the
   * docIDs out of order.</p>
   *
   * <p> Many collectors don't mind getting docIDs out of
   * order, so it's important to return <code>true</code>
   * here.
   */
  public abstract boolean acceptsDocsOutOfOrder();
  
 }
diff --git a/lucene/core/src/java/org/apache/lucene/search/ConstantScoreQuery.java b/lucene/core/src/java/org/apache/lucene/search/ConstantScoreQuery.java
index a917a0c5cd2..2b7f4ed19b3 100644
-- a/lucene/core/src/java/org/apache/lucene/search/ConstantScoreQuery.java
++ b/lucene/core/src/java/org/apache/lucene/search/ConstantScoreQuery.java
@@ -212,31 +212,16 @@ public class ConstantScoreQuery extends Query {
     }
 
     @Override
    public boolean score(Collector collector, int max) throws IOException {
    public boolean score(LeafCollector collector, int max) throws IOException {
       return bulkScorer.score(wrapCollector(collector), max);
     }
 
    private Collector wrapCollector(final Collector collector) {
      return new Collector() {
    private LeafCollector wrapCollector(LeafCollector collector) {
      return new FilterLeafCollector(collector) {
         @Override
         public void setScorer(Scorer scorer) throws IOException {
           // we must wrap again here, but using the scorer passed in as parameter:
          collector.setScorer(new ConstantScorer(scorer, weight, theScore));
        }
        
        @Override
        public void collect(int doc) throws IOException {
          collector.collect(doc);
        }
        
        @Override
        public void setNextReader(AtomicReaderContext context) throws IOException {
          collector.setNextReader(context);
        }
        
        @Override
        public boolean acceptsDocsOutOfOrder() {
          return collector.acceptsDocsOutOfOrder();
          in.setScorer(new ConstantScorer(scorer, weight, theScore));
         }
       };
     }
diff --git a/lucene/core/src/java/org/apache/lucene/search/FakeScorer.java b/lucene/core/src/java/org/apache/lucene/search/FakeScorer.java
index 89b92a5bb22..e2a50c8f37d 100644
-- a/lucene/core/src/java/org/apache/lucene/search/FakeScorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/FakeScorer.java
@@ -20,7 +20,7 @@ package org.apache.lucene.search;
 import java.util.Collection;
 
 /** Used by {@link BulkScorer}s that need to pass a {@link
 *  Scorer} to {@link Collector#setScorer}. */
 *  Scorer} to {@link LeafCollector#setScorer}. */
 final class FakeScorer extends Scorer {
   float score;
   int doc = -1;
diff --git a/lucene/core/src/java/org/apache/lucene/search/FilterCollector.java b/lucene/core/src/java/org/apache/lucene/search/FilterCollector.java
new file mode 100644
index 00000000000..247bb038531
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/search/FilterCollector.java
@@ -0,0 +1,48 @@
package org.apache.lucene.search;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;

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

/**
 * {@link Collector} delegator.
 *
 * @lucene.experimental
 */
public class FilterCollector implements Collector {

  protected final Collector in;

  /** Sole constructor. */
  public FilterCollector(Collector in) {
    this.in = in;
  }

  @Override
  public LeafCollector getLeafCollector(AtomicReaderContext context) throws IOException {
    return in.getLeafCollector(context);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + in + ")";
  }
  
}
diff --git a/lucene/core/src/java/org/apache/lucene/search/FilterLeafCollector.java b/lucene/core/src/java/org/apache/lucene/search/FilterLeafCollector.java
new file mode 100644
index 00000000000..e3ae9a8b0ab
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/search/FilterLeafCollector.java
@@ -0,0 +1,56 @@
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

/**
 * {@link LeafCollector} delegator.
 *
 * @lucene.experimental
 */
public class FilterLeafCollector implements LeafCollector {

  protected final LeafCollector in;

  /** Sole constructor. */
  public FilterLeafCollector(LeafCollector in) {
    this.in = in;
  }

  @Override
  public void setScorer(Scorer scorer) throws IOException {
    in.setScorer(scorer);
  }

  @Override
  public void collect(int doc) throws IOException {
    in.collect(doc);
  }

  @Override
  public boolean acceptsDocsOutOfOrder() {
    return in.acceptsDocsOutOfOrder();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + in + ")";
  }

}
diff --git a/lucene/core/src/java/org/apache/lucene/search/FilteredQuery.java b/lucene/core/src/java/org/apache/lucene/search/FilteredQuery.java
index f4ef5944404..d700a3011f5 100644
-- a/lucene/core/src/java/org/apache/lucene/search/FilteredQuery.java
++ b/lucene/core/src/java/org/apache/lucene/search/FilteredQuery.java
@@ -225,7 +225,7 @@ public class FilteredQuery extends Query {
     }
 
     @Override
    public boolean score(Collector collector, int maxDoc) throws IOException {
    public boolean score(LeafCollector collector, int maxDoc) throws IOException {
       // the normalization trick already applies the boost of this query,
       // so we can use the wrapped scorer directly:
       collector.setScorer(scorer);
diff --git a/lucene/core/src/java/org/apache/lucene/search/IndexSearcher.java b/lucene/core/src/java/org/apache/lucene/search/IndexSearcher.java
index 8b33ae736e5..8f1a5f61dbd 100644
-- a/lucene/core/src/java/org/apache/lucene/search/IndexSearcher.java
++ b/lucene/core/src/java/org/apache/lucene/search/IndexSearcher.java
@@ -275,7 +275,7 @@ public class IndexSearcher {
 
   /** Lower-level search API.
    *
   * <p>{@link Collector#collect(int)} is called for every matching
   * <p>{@link LeafCollector#collect(int)} is called for every matching
    * document.
    *
    * @param query to match documents
@@ -291,7 +291,7 @@ public class IndexSearcher {
 
   /** Lower-level search API.
    *
   * <p>{@link Collector#collect(int)} is called for every matching document.
   * <p>{@link LeafCollector#collect(int)} is called for every matching document.
    *
    * @throws BooleanQuery.TooManyClauses If a query would exceed 
    *         {@link BooleanQuery#getMaxClauseCount()} clauses.
@@ -578,7 +578,7 @@ public class IndexSearcher {
    * Lower-level search API.
    * 
    * <p>
   * {@link Collector#collect(int)} is called for every document. <br>
   * {@link LeafCollector#collect(int)} is called for every document. <br>
    * 
    * <p>
    * NOTE: this method executes the searches on all given leaves exclusively.
@@ -600,17 +600,18 @@ public class IndexSearcher {
     // threaded...?  the Collector could be sync'd?
     // always use single thread:
     for (AtomicReaderContext ctx : leaves) { // search each subreader
      final LeafCollector leafCollector;
       try {
        collector.setNextReader(ctx);
        leafCollector = collector.getLeafCollector(ctx);
       } catch (CollectionTerminatedException e) {
         // there is no doc of interest in this reader context
         // continue with the following leaf
         continue;
       }
      BulkScorer scorer = weight.bulkScorer(ctx, !collector.acceptsDocsOutOfOrder(), ctx.reader().getLiveDocs());
      BulkScorer scorer = weight.bulkScorer(ctx, !leafCollector.acceptsDocsOutOfOrder(), ctx.reader().getLiveDocs());
       if (scorer != null) {
         try {
          scorer.score(collector);
          scorer.score(leafCollector);
         } catch (CollectionTerminatedException e) {
           // collection was terminated prematurely
           // continue with the following leaf
@@ -779,12 +780,12 @@ public class IndexSearcher {
       try {
         final AtomicReaderContext ctx = slice.leaves[0];
         final int base = ctx.docBase;
        hq.setNextReader(ctx);
        hq.setScorer(fakeScorer);
        final LeafCollector collector = hq.getLeafCollector(ctx);
        collector.setScorer(fakeScorer);
         for(ScoreDoc scoreDoc : docs.scoreDocs) {
           fakeScorer.doc = scoreDoc.doc - base;
           fakeScorer.score = scoreDoc.score;
          hq.collect(scoreDoc.doc-base);
          collector.collect(scoreDoc.doc-base);
         }
 
         // Carry over maxScore from sub:
diff --git a/lucene/core/src/java/org/apache/lucene/search/LeafCollector.java b/lucene/core/src/java/org/apache/lucene/search/LeafCollector.java
new file mode 100644
index 00000000000..562e76dde76
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/search/LeafCollector.java
@@ -0,0 +1,121 @@
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

import org.apache.lucene.index.AtomicReaderContext;

/**
 * <p>Collector decouples the score from the collected doc:
 * the score computation is skipped entirely if it's not
 * needed.  Collectors that do need the score should
 * implement the {@link #setScorer} method, to hold onto the
 * passed {@link Scorer} instance, and call {@link
 * Scorer#score()} within the collect method to compute the
 * current hit's score.  If your collector may request the
 * score for a single hit multiple times, you should use
 * {@link ScoreCachingWrappingScorer}. </p>
 * 
 * <p><b>NOTE:</b> The doc that is passed to the collect
 * method is relative to the current reader. If your
 * collector needs to resolve this to the docID space of the
 * Multi*Reader, you must re-base it by recording the
 * docBase from the most recent setNextReader call.  Here's
 * a simple example showing how to collect docIDs into a
 * BitSet:</p>
 * 
 * <pre class="prettyprint">
 * IndexSearcher searcher = new IndexSearcher(indexReader);
 * final BitSet bits = new BitSet(indexReader.maxDoc());
 * searcher.search(query, new Collector() {
 *
 *   public LeafCollector getLeafCollector(AtomicReaderContext context)
 *       throws IOException {
 *     final int docBase = context.docBase;
 *     return new LeafCollector() {
 *
 *       <em>// ignore scorer</em>
 *       public void setScorer(Scorer scorer) throws IOException {
 *       }
 *
 *       public void collect(int doc) throws IOException {
 *         bits.set(docBase + doc);
 *       }
 *
 *       // accept docs out of order (for a BitSet it doesn't matter)
 *       public boolean acceptsDocsOutOfOrder() {
 *         return true;
 *       }
 *          
 *     };
 *   }
 *      
 * });
 * </pre>
 *
 * <p>Not all collectors will need to rebase the docID.  For
 * example, a collector that simply counts the total number
 * of hits would skip it.</p>
 *
 * @lucene.experimental
 */
public interface LeafCollector {

  /**
   * Called before successive calls to {@link #collect(int)}. Implementations
   * that need the score of the current document (passed-in to
   * {@link #collect(int)}), should save the passed-in Scorer and call
   * scorer.score() when needed.
   */
  void setScorer(Scorer scorer) throws IOException;
  
  /**
   * Called once for every document matching a query, with the unbased document
   * number.
   * <p>Note: The collection of the current segment can be terminated by throwing
   * a {@link CollectionTerminatedException}. In this case, the last docs of the
   * current {@link AtomicReaderContext} will be skipped and {@link IndexSearcher}
   * will swallow the exception and continue collection with the next leaf.
   * <p>
   * Note: This is called in an inner search loop. For good search performance,
   * implementations of this method should not call {@link IndexSearcher#doc(int)} or
   * {@link org.apache.lucene.index.IndexReader#document(int)} on every hit.
   * Doing so can slow searches by an order of magnitude or more.
   */
  void collect(int doc) throws IOException;

  /**
   * Return <code>true</code> if this collector does not
   * require the matching docIDs to be delivered in int sort
   * order (smallest to largest) to {@link #collect}.
   *
   * <p> Most Lucene Query implementations will visit
   * matching docIDs in order.  However, some queries
   * (currently limited to certain cases of {@link
   * BooleanQuery}) can achieve faster searching if the
   * <code>Collector</code> allows them to deliver the
   * docIDs out of order.</p>
   *
   * <p> Many collectors don't mind getting docIDs out of
   * order, so it's important to return <code>true</code>
   * here.
   */
  boolean acceptsDocsOutOfOrder();

}
diff --git a/lucene/core/src/java/org/apache/lucene/search/MultiCollector.java b/lucene/core/src/java/org/apache/lucene/search/MultiCollector.java
index 40c0838d72a..859b8932567 100644
-- a/lucene/core/src/java/org/apache/lucene/search/MultiCollector.java
++ b/lucene/core/src/java/org/apache/lucene/search/MultiCollector.java
@@ -18,6 +18,7 @@ package org.apache.lucene.search;
  */
 
 import java.io.IOException;
import java.util.Arrays;
 
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.search.Collector;
@@ -29,7 +30,12 @@ import org.apache.lucene.search.Scorer;
  * list of collectors and wraps them with {@link MultiCollector}, while
  * filtering out the <code>null</code> null ones.
  */
public class MultiCollector extends Collector {
public class MultiCollector implements Collector {

  /** See {@link #wrap(Iterable)}. */
  public static Collector wrap(Collector... collectors) {
    return wrap(Arrays.asList(collectors));
  }
 
   /**
    * Wraps a list of {@link Collector}s with a {@link MultiCollector}. This
@@ -47,7 +53,7 @@ public class MultiCollector extends Collector {
    *           if either 0 collectors were input, or all collectors are
    *           <code>null</code>.
    */
  public static Collector wrap(Collector... collectors) {
  public static Collector wrap(Iterable<? extends Collector> collectors) {
     // For the user's convenience, we allow null collectors to be passed.
     // However, to improve performance, these null collectors are found
     // and dropped from the array we save for actual collection time.
@@ -70,8 +76,6 @@ public class MultiCollector extends Collector {
         }
       }
       return col;
    } else if (n == collectors.length) {
      return new MultiCollector(collectors);
     } else {
       Collector[] colls = new Collector[n];
       n = 0;
@@ -91,34 +95,47 @@ public class MultiCollector extends Collector {
   }
 
   @Override
  public boolean acceptsDocsOutOfOrder() {
    for (Collector c : collectors) {
      if (!c.acceptsDocsOutOfOrder()) {
        return false;
      }
  public LeafCollector getLeafCollector(AtomicReaderContext context) throws IOException {
    final LeafCollector[] leafCollectors = new LeafCollector[collectors.length];
    for (int i = 0; i < collectors.length; ++i) {
      leafCollectors[i] = collectors[i].getLeafCollector(context);
     }
    return true;
    return new MultiLeafCollector(leafCollectors);
   }
 
  @Override
  public void collect(int doc) throws IOException {
    for (Collector c : collectors) {
      c.collect(doc);

  private static class MultiLeafCollector implements LeafCollector {

    private final LeafCollector[] collectors;

    private MultiLeafCollector(LeafCollector[] collectors) {
      this.collectors = collectors;
     }
  }
 
  @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
    for (Collector c : collectors) {
      c.setNextReader(context);
    @Override
    public void setScorer(Scorer scorer) throws IOException {
      for (LeafCollector c : collectors) {
        c.setScorer(scorer);
      }
     }
  }
 
  @Override
  public void setScorer(Scorer s) throws IOException {
    for (Collector c : collectors) {
      c.setScorer(s);
    @Override
    public void collect(int doc) throws IOException {
      for (LeafCollector c : collectors) {
        c.collect(doc);
      }
    }

    @Override
    public boolean acceptsDocsOutOfOrder() {
      for (LeafCollector c : collectors) {
        if (!c.acceptsDocsOutOfOrder()) {
          return false;
        }
      }
      return true;
     }

   }
 
 }
diff --git a/lucene/core/src/java/org/apache/lucene/search/PositiveScoresOnlyCollector.java b/lucene/core/src/java/org/apache/lucene/search/PositiveScoresOnlyCollector.java
index d35a755e2f0..ba222951cbd 100644
-- a/lucene/core/src/java/org/apache/lucene/search/PositiveScoresOnlyCollector.java
++ b/lucene/core/src/java/org/apache/lucene/search/PositiveScoresOnlyCollector.java
@@ -26,38 +26,33 @@ import org.apache.lucene.index.AtomicReaderContext;
  * {@link Collector} and makes sure only documents with
  * scores &gt; 0 are collected.
  */
public class PositiveScoresOnlyCollector extends Collector {
public class PositiveScoresOnlyCollector extends FilterCollector {
 
  final private Collector c;
  private Scorer scorer;
  
  public PositiveScoresOnlyCollector(Collector c) {
    this.c = c;
  }
  
  @Override
  public void collect(int doc) throws IOException {
    if (scorer.score() > 0) {
      c.collect(doc);
    }
  }

  @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
    c.setNextReader(context);
  }

  @Override
  public void setScorer(Scorer scorer) throws IOException {
    // Set a ScoreCachingWrappingScorer in case the wrapped Collector will call
    // score() also.
    this.scorer = new ScoreCachingWrappingScorer(scorer);
    c.setScorer(this.scorer);
  public PositiveScoresOnlyCollector(Collector in) {
    super(in);
   }
 
   @Override
  public boolean acceptsDocsOutOfOrder() {
    return c.acceptsDocsOutOfOrder();
  public LeafCollector getLeafCollector(AtomicReaderContext context)
      throws IOException {
    return new FilterLeafCollector(super.getLeafCollector(context)) {

      private Scorer scorer;

      @Override
      public void setScorer(Scorer scorer) throws IOException {
        this.scorer = new ScoreCachingWrappingScorer(scorer);
        in.setScorer(this.scorer);
      }

      @Override
      public void collect(int doc) throws IOException {
        if (scorer.score() > 0) {
          in.collect(doc);
        }
      }
      
    };
   }
 
 }
diff --git a/lucene/core/src/java/org/apache/lucene/search/ScoreCachingWrappingScorer.java b/lucene/core/src/java/org/apache/lucene/search/ScoreCachingWrappingScorer.java
index 471dc20e123..844290c856a 100644
-- a/lucene/core/src/java/org/apache/lucene/search/ScoreCachingWrappingScorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/ScoreCachingWrappingScorer.java
@@ -37,7 +37,7 @@ public class ScoreCachingWrappingScorer extends Scorer {
   private final Scorer scorer;
   private int curDoc = -1;
   private float curScore;
  

   /** Creates a new instance by wrapping the given scorer. */
   public ScoreCachingWrappingScorer(Scorer scorer) {
     super(scorer.weight);
@@ -51,7 +51,7 @@ public class ScoreCachingWrappingScorer extends Scorer {
       curScore = scorer.score();
       curDoc = doc;
     }
    

     return curScore;
   }
 
diff --git a/lucene/core/src/java/org/apache/lucene/search/Scorer.java b/lucene/core/src/java/org/apache/lucene/search/Scorer.java
index abcbb616c84..929d3b9a65f 100644
-- a/lucene/core/src/java/org/apache/lucene/search/Scorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/Scorer.java
@@ -57,7 +57,7 @@ public abstract class Scorer extends DocsEnum {
   /** Returns the score of the current document matching the query.
    * Initially invalid, until {@link #nextDoc()} or {@link #advance(int)}
    * is called the first time, or when called from within
   * {@link Collector#collect}.
   * {@link LeafCollector#collect}.
    */
   public abstract float score() throws IOException;
   
diff --git a/lucene/core/src/java/org/apache/lucene/search/SimpleCollector.java b/lucene/core/src/java/org/apache/lucene/search/SimpleCollector.java
new file mode 100644
index 00000000000..5803b2e4dee
-- /dev/null
++ b/lucene/core/src/java/org/apache/lucene/search/SimpleCollector.java
@@ -0,0 +1,53 @@
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

import org.apache.lucene.index.AtomicReaderContext;

/**
 * Base {@link Collector} implementation that is used to collect all contexts.
 *
 * @lucene.experimental
 */
public abstract class SimpleCollector implements Collector, LeafCollector {

  @Override
  public final LeafCollector getLeafCollector(AtomicReaderContext context) throws IOException {
    doSetNextReader(context);
    return this;
  }

  /** This method is called before collecting <code>context</code>. */
  protected void doSetNextReader(AtomicReaderContext context) throws IOException {}

  @Override
  public void setScorer(Scorer scorer) throws IOException {
    // no-op by default
  }

  // redeclare methods so that javadocs are inherited on sub-classes

  @Override
  public abstract boolean acceptsDocsOutOfOrder();

  @Override
  public abstract void collect(int doc) throws IOException;

}
diff --git a/lucene/core/src/java/org/apache/lucene/search/SortRescorer.java b/lucene/core/src/java/org/apache/lucene/search/SortRescorer.java
index 1bb21343c40..6f125e8a5c3 100644
-- a/lucene/core/src/java/org/apache/lucene/search/SortRescorer.java
++ b/lucene/core/src/java/org/apache/lucene/search/SortRescorer.java
@@ -75,7 +75,7 @@ public class SortRescorer extends Rescorer {
 
       if (readerContext != null) {
         // We advanced to another segment:
        collector.setNextReader(readerContext);
        collector.getLeafCollector(readerContext);
         collector.setScorer(fakeScorer);
         docBase = readerContext.docBase;
       }
diff --git a/lucene/core/src/java/org/apache/lucene/search/TimeLimitingCollector.java b/lucene/core/src/java/org/apache/lucene/search/TimeLimitingCollector.java
index 2d2eb0ee948..9a08a2b4b8f 100644
-- a/lucene/core/src/java/org/apache/lucene/search/TimeLimitingCollector.java
++ b/lucene/core/src/java/org/apache/lucene/search/TimeLimitingCollector.java
@@ -29,7 +29,7 @@ import java.io.IOException;
  * exceeded, the search thread is stopped by throwing a
  * {@link TimeExceededException}.
  */
public class TimeLimitingCollector extends Collector {
public class TimeLimitingCollector implements Collector {
 
 
   /** Thrown when elapsed search time exceeds allowed search time. */
@@ -131,45 +131,30 @@ public class TimeLimitingCollector extends Collector {
     this.greedy = greedy;
   }
   
  /**
   * Calls {@link Collector#collect(int)} on the decorated {@link Collector}
   * unless the allowed time has passed, in which case it throws an exception.
   * 
   * @throws TimeExceededException
   *           if the time allowed has exceeded.
   */
  @Override
  public void collect(final int doc) throws IOException {
    final long time = clock.get();
    if (timeout < time) {
      if (greedy) {
        //System.out.println(this+"  greedy: before failing, collecting doc: "+(docBase + doc)+"  "+(time-t0));
        collector.collect(doc);
      }
      //System.out.println(this+"  failing on:  "+(docBase + doc)+"  "+(time-t0));
      throw new TimeExceededException( timeout-t0, time-t0, docBase + doc );
    }
    //System.out.println(this+"  collecting: "+(docBase + doc)+"  "+(time-t0));
    collector.collect(doc);
  }
  
   @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
    collector.setNextReader(context);
  public LeafCollector getLeafCollector(AtomicReaderContext context) throws IOException {
     this.docBase = context.docBase;
     if (Long.MIN_VALUE == t0) {
       setBaseline();
     }
  }
  
  @Override
  public void setScorer(Scorer scorer) throws IOException {
    collector.setScorer(scorer);
  }

  @Override
  public boolean acceptsDocsOutOfOrder() {
    return collector.acceptsDocsOutOfOrder();
    return new FilterLeafCollector(collector.getLeafCollector(context)) {
      
      @Override
      public void collect(int doc) throws IOException {
        final long time = clock.get();
        if (timeout < time) {
          if (greedy) {
            //System.out.println(this+"  greedy: before failing, collecting doc: "+(docBase + doc)+"  "+(time-t0));
            in.collect(doc);
          }
          //System.out.println(this+"  failing on:  "+(docBase + doc)+"  "+(time-t0));
          throw new TimeExceededException( timeout-t0, time-t0, docBase + doc );
        }
        //System.out.println(this+"  collecting: "+(docBase + doc)+"  "+(time-t0));
        in.collect(doc);
      }
      
    };
   }
   
   /**
diff --git a/lucene/core/src/java/org/apache/lucene/search/TopDocsCollector.java b/lucene/core/src/java/org/apache/lucene/search/TopDocsCollector.java
index 5e7dd50406f..bd0687e9e16 100644
-- a/lucene/core/src/java/org/apache/lucene/search/TopDocsCollector.java
++ b/lucene/core/src/java/org/apache/lucene/search/TopDocsCollector.java
@@ -31,7 +31,7 @@ import org.apache.lucene.util.PriorityQueue;
  * however, you might want to consider overriding all methods, in order to avoid
  * a NullPointerException.
  */
public abstract class TopDocsCollector<T extends ScoreDoc> extends Collector {
public abstract class TopDocsCollector<T extends ScoreDoc> extends SimpleCollector {
 
   /** This is used in case topDocs() is called with illegal parameters, or there
    *  simply aren't (enough) results. */
diff --git a/lucene/core/src/java/org/apache/lucene/search/TopFieldCollector.java b/lucene/core/src/java/org/apache/lucene/search/TopFieldCollector.java
index 3f1fa156230..6f038c41786 100644
-- a/lucene/core/src/java/org/apache/lucene/search/TopFieldCollector.java
++ b/lucene/core/src/java/org/apache/lucene/search/TopFieldCollector.java
@@ -92,7 +92,7 @@ public abstract class TopFieldCollector extends TopDocsCollector<Entry> {
     }
     
     @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       this.docBase = context.docBase;
       queue.setComparator(0, comparator.setNextReader(context));
       comparator = queue.firstComparator;
@@ -446,7 +446,7 @@ public abstract class TopFieldCollector extends TopDocsCollector<Entry> {
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       docBase = context.docBase;
       for (int i = 0; i < comparators.length; i++) {
         queue.setComparator(i, comparators[i].setNextReader(context));
@@ -1001,7 +1001,7 @@ public abstract class TopFieldCollector extends TopDocsCollector<Entry> {
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       docBase = context.docBase;
       afterDoc = after.doc - docBase;
       for (int i = 0; i < comparators.length; i++) {
diff --git a/lucene/core/src/java/org/apache/lucene/search/TopScoreDocCollector.java b/lucene/core/src/java/org/apache/lucene/search/TopScoreDocCollector.java
index 0674779f718..bfebeda930d 100644
-- a/lucene/core/src/java/org/apache/lucene/search/TopScoreDocCollector.java
++ b/lucene/core/src/java/org/apache/lucene/search/TopScoreDocCollector.java
@@ -113,9 +113,9 @@ public abstract class TopScoreDocCollector extends TopDocsCollector<ScoreDoc> {
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) {
      super.setNextReader(context);
      afterDoc = after.doc - docBase;
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
      super.doSetNextReader(context);
      afterDoc = after.doc - context.docBase;
     }
 
     @Override
@@ -208,9 +208,9 @@ public abstract class TopScoreDocCollector extends TopDocsCollector<ScoreDoc> {
     }
     
     @Override
    public void setNextReader(AtomicReaderContext context) {
      super.setNextReader(context);
      afterDoc = after.doc - docBase;
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
      super.doSetNextReader(context);
      afterDoc = after.doc - context.docBase;
     }
     
     @Override
@@ -300,7 +300,7 @@ public abstract class TopScoreDocCollector extends TopDocsCollector<ScoreDoc> {
   }
   
   @Override
  public void setNextReader(AtomicReaderContext context) {
  protected void doSetNextReader(AtomicReaderContext context) throws IOException {
     docBase = context.docBase;
   }
   
diff --git a/lucene/core/src/java/org/apache/lucene/search/TotalHitCountCollector.java b/lucene/core/src/java/org/apache/lucene/search/TotalHitCountCollector.java
index 1704d8b701c..4fc5be65002 100644
-- a/lucene/core/src/java/org/apache/lucene/search/TotalHitCountCollector.java
++ b/lucene/core/src/java/org/apache/lucene/search/TotalHitCountCollector.java
@@ -17,13 +17,12 @@ package org.apache.lucene.search;
  * limitations under the License.
  */
 
import org.apache.lucene.index.AtomicReaderContext;
 
 /**
  * Just counts the total number of hits.
  */
 
public class TotalHitCountCollector extends Collector {
public class TotalHitCountCollector extends SimpleCollector {
   private int totalHits;
 
   /** Returns how many hits matched the search. */
@@ -31,19 +30,11 @@ public class TotalHitCountCollector extends Collector {
     return totalHits;
   }
 
  @Override
  public void setScorer(Scorer scorer) {
  }

   @Override
   public void collect(int doc) {
     totalHits++;
   }
 
  @Override
  public void setNextReader(AtomicReaderContext context) {
  }

   @Override
   public boolean acceptsDocsOutOfOrder() {
     return true;
diff --git a/lucene/core/src/java/org/apache/lucene/search/Weight.java b/lucene/core/src/java/org/apache/lucene/search/Weight.java
index 696c7ab764a..0603cd8d478 100644
-- a/lucene/core/src/java/org/apache/lucene/search/Weight.java
++ b/lucene/core/src/java/org/apache/lucene/search/Weight.java
@@ -150,7 +150,7 @@ public abstract class Weight {
     }
 
     @Override
    public boolean score(Collector collector, int max) throws IOException {
    public boolean score(LeafCollector collector, int max) throws IOException {
       // TODO: this may be sort of weird, when we are
       // embedded in a BooleanScorer, because we are
       // called for every chunk of 2048 documents.  But,
@@ -172,7 +172,7 @@ public abstract class Weight {
   /**
    * Returns true iff this implementation scores docs only out of order. This
    * method is used in conjunction with {@link Collector}'s
   * {@link Collector#acceptsDocsOutOfOrder() acceptsDocsOutOfOrder} and
   * {@link LeafCollector#acceptsDocsOutOfOrder() acceptsDocsOutOfOrder} and
    * {@link #bulkScorer(AtomicReaderContext, boolean, Bits)} to
    * create a matching {@link Scorer} instance for a given {@link Collector}, or
    * vice versa.
diff --git a/lucene/core/src/java/org/apache/lucene/search/package.html b/lucene/core/src/java/org/apache/lucene/search/package.html
index 51e199e025d..889501a4941 100644
-- a/lucene/core/src/java/org/apache/lucene/search/package.html
++ b/lucene/core/src/java/org/apache/lucene/search/package.html
@@ -508,7 +508,7 @@ on the built-in available scoring models and extending or changing Similarity.
         abstract method:
         <ol>
             <li>
                {@link org.apache.lucene.search.BulkScorer#score(org.apache.lucene.search.Collector,int) score(Collector,int)} &mdash;
                {@link org.apache.lucene.search.BulkScorer#score(org.apache.lucene.search.LeafCollector,int) score(LeafCollector,int)} &mdash;
 		Score all documents up to but not including the specified max document.
 	    </li>
         </ol>
@@ -563,7 +563,7 @@ on the built-in available scoring models and extending or changing Similarity.
 <p>If a Filter is being used, some initial setup is done to determine which docs to include. 
    Otherwise, we ask the Weight for a {@link org.apache.lucene.search.Scorer Scorer} for each
    {@link org.apache.lucene.index.IndexReader IndexReader} segment and proceed by calling
   {@link org.apache.lucene.search.BulkScorer#score(org.apache.lucene.search.Collector) BulkScorer.score(Collector)}.
   {@link org.apache.lucene.search.BulkScorer#score(org.apache.lucene.search.LeafCollector) BulkScorer.score(LeafCollector)}.
 </p>
 <p>At last, we are actually going to score some documents. The score method takes in the Collector
    (most likely the TopScoreDocCollector or TopFieldCollector) and does its business.Of course, here 
diff --git a/lucene/core/src/test/org/apache/lucene/index/TestOmitTf.java b/lucene/core/src/test/org/apache/lucene/index/TestOmitTf.java
index 47abea28b83..04fad029d86 100644
-- a/lucene/core/src/test/org/apache/lucene/index/TestOmitTf.java
++ b/lucene/core/src/test/org/apache/lucene/index/TestOmitTf.java
@@ -26,6 +26,7 @@ import org.apache.lucene.document.Field;
 import org.apache.lucene.document.FieldType;
 import org.apache.lucene.document.TextField;
 import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.BooleanClause.Occur;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.CollectionStatistics;
@@ -34,6 +35,7 @@ import org.apache.lucene.search.Explanation;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.PhraseQuery;
 import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
 import org.apache.lucene.search.TermQuery;
 import org.apache.lucene.search.TermStatistics;
 import org.apache.lucene.search.similarities.TFIDFSimilarity;
@@ -414,14 +416,12 @@ public class TestOmitTf extends LuceneTestCase {
     dir.close();
   }
      
  public static class CountingHitCollector extends Collector {
  public static class CountingHitCollector extends SimpleCollector {
     static int count=0;
     static int sum=0;
     private int docBase = -1;
     CountingHitCollector(){count=0;sum=0;}
     @Override
    public void setScorer(Scorer scorer) throws IOException {}
    @Override
     public void collect(int doc) throws IOException {
       count++;
       sum += doc + docBase;  // use it to avoid any possibility of being merged away
@@ -431,7 +431,7 @@ public class TestOmitTf extends LuceneTestCase {
     public static int getSum() { return sum; }
     
     @Override
    public void setNextReader(AtomicReaderContext context) {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       docBase = context.docBase;
     }
     @Override
diff --git a/lucene/core/src/test/org/apache/lucene/search/JustCompileSearch.java b/lucene/core/src/test/org/apache/lucene/search/JustCompileSearch.java
index ee4775946ae..f09d992e0fc 100644
-- a/lucene/core/src/test/org/apache/lucene/search/JustCompileSearch.java
++ b/lucene/core/src/test/org/apache/lucene/search/JustCompileSearch.java
@@ -17,6 +17,8 @@ package org.apache.lucene.search;
  * limitations under the License.
  */
 
import java.io.IOException;

 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.Terms;
 import org.apache.lucene.index.TermsEnum;
@@ -37,7 +39,7 @@ final class JustCompileSearch {
 
   private static final String UNSUPPORTED_MSG = "unsupported: used for back-compat testing only !";
 
  static final class JustCompileCollector extends Collector {
  static final class JustCompileCollector extends SimpleCollector {
 
     @Override
     public void collect(int doc) {
@@ -45,7 +47,7 @@ final class JustCompileSearch {
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       throw new UnsupportedOperationException(UNSUPPORTED_MSG);
     }
 
@@ -290,7 +292,7 @@ final class JustCompileSearch {
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       throw new UnsupportedOperationException(UNSUPPORTED_MSG);
     }
 
diff --git a/lucene/core/src/test/org/apache/lucene/search/MultiCollectorTest.java b/lucene/core/src/test/org/apache/lucene/search/MultiCollectorTest.java
index 2a63f49849f..5a7df3c1c12 100644
-- a/lucene/core/src/test/org/apache/lucene/search/MultiCollectorTest.java
++ b/lucene/core/src/test/org/apache/lucene/search/MultiCollectorTest.java
@@ -27,7 +27,7 @@ import org.junit.Test;
 
 public class MultiCollectorTest extends LuceneTestCase {
 
  private static class DummyCollector extends Collector {
  private static class DummyCollector extends SimpleCollector {
 
     boolean acceptsDocsOutOfOrderCalled = false;
     boolean collectCalled = false;
@@ -46,7 +46,7 @@ public class MultiCollectorTest extends LuceneTestCase {
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       setNextReaderCalled = true;
     }
 
@@ -71,10 +71,11 @@ public class MultiCollectorTest extends LuceneTestCase {
     // doesn't, an NPE would be thrown.
     Collector c = MultiCollector.wrap(new DummyCollector(), null, new DummyCollector());
     assertTrue(c instanceof MultiCollector);
    assertTrue(c.acceptsDocsOutOfOrder());
    c.collect(1);
    c.setNextReader(null);
    c.setScorer(null);
    final LeafCollector ac = c.getLeafCollector(null);
    assertTrue(ac.acceptsDocsOutOfOrder());
    ac.collect(1);
    c.getLeafCollector(null);
    c.getLeafCollector(null).setScorer(null);
   }
 
   @Test
@@ -93,10 +94,11 @@ public class MultiCollectorTest extends LuceneTestCase {
     // doesn't, an NPE would be thrown.
     DummyCollector[] dcs = new DummyCollector[] { new DummyCollector(), new DummyCollector() };
     Collector c = MultiCollector.wrap(dcs);
    assertTrue(c.acceptsDocsOutOfOrder());
    c.collect(1);
    c.setNextReader(null);
    c.setScorer(null);
    LeafCollector ac = c.getLeafCollector(null);
    assertTrue(ac.acceptsDocsOutOfOrder());
    ac.collect(1);
    ac = c.getLeafCollector(null);
    ac.setScorer(null);
 
     for (DummyCollector dc : dcs) {
       assertTrue(dc.acceptsDocsOutOfOrderCalled);
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestBooleanOr.java b/lucene/core/src/test/org/apache/lucene/search/TestBooleanOr.java
index b1ba0f1fbc8..137b4ce1dc9 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestBooleanOr.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestBooleanOr.java
@@ -187,10 +187,7 @@ public class TestBooleanOr extends LuceneTestCase {
 
     final FixedBitSet hits = new FixedBitSet(docCount);
     final AtomicInteger end = new AtomicInteger();
    Collector c = new Collector() {
        @Override
        public void setNextReader(AtomicReaderContext sub) {
        }
    LeafCollector c = new SimpleCollector() {
 
         @Override
         public void collect(int doc) {
@@ -198,10 +195,6 @@ public class TestBooleanOr extends LuceneTestCase {
           hits.set(doc);
         }
 
        @Override
        public void setScorer(Scorer scorer) {
        }

         @Override
         public boolean acceptsDocsOutOfOrder() {
           return true;
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestBooleanQueryVisitSubscorers.java b/lucene/core/src/test/org/apache/lucene/search/TestBooleanQueryVisitSubscorers.java
index 175061a4281..5e61f6981b1 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestBooleanQueryVisitSubscorers.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestBooleanQueryVisitSubscorers.java
@@ -121,46 +121,45 @@ public class TestBooleanQueryVisitSubscorers extends LuceneTestCase {
     return collector.docCounts;
   }
   
  static class MyCollector extends Collector {
    
    private TopDocsCollector<ScoreDoc> collector;
    private int docBase;
  static class MyCollector extends FilterCollector {
 
     public final Map<Integer,Integer> docCounts = new HashMap<>();
     private final Set<Scorer> tqsSet = new HashSet<>();
     
     MyCollector() {
      collector = TopScoreDocCollector.create(10, true);
    }

    @Override
    public boolean acceptsDocsOutOfOrder() {
      return false;
      super(TopScoreDocCollector.create(10, true));
     }
 
    @Override
    public void collect(int doc) throws IOException {
      int freq = 0;
      for(Scorer scorer : tqsSet) {
        if (doc == scorer.docID()) {
          freq += scorer.freq();
    public LeafCollector getLeafCollector(AtomicReaderContext context)
        throws IOException {
      final int docBase = context.docBase;
      return new FilterLeafCollector(super.getLeafCollector(context)) {
        
        @Override
        public boolean acceptsDocsOutOfOrder() {
          return false;
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
        
        @Override
        public void setScorer(Scorer scorer) throws IOException {
          super.setScorer(scorer);
          tqsSet.clear();
          fillLeaves(scorer, tqsSet);
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
          super.collect(doc);
        }
        
      };
     }
     
     private void fillLeaves(Scorer scorer, Set<Scorer> set) {
@@ -174,11 +173,12 @@ public class TestBooleanQueryVisitSubscorers extends LuceneTestCase {
     }
     
     public TopDocs topDocs(){
      return collector.topDocs();
      return ((TopDocsCollector<?>) in).topDocs();
     }
     
     public int freq(int doc) throws IOException {
       return docCounts.get(doc);
     }
    
   }
 }
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestBooleanScorer.java b/lucene/core/src/test/org/apache/lucene/search/TestBooleanScorer.java
index 68227c6df11..e6ed13aa21c 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestBooleanScorer.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestBooleanScorer.java
@@ -84,7 +84,7 @@ public class TestBooleanScorer extends LuceneTestCase {
       private int doc = -1;
 
       @Override
      public boolean score(Collector c, int maxDoc) throws IOException {
      public boolean score(LeafCollector c, int maxDoc) throws IOException {
         assert doc == -1;
         doc = 3000;
         FakeScorer fs = new FakeScorer();
@@ -99,7 +99,7 @@ public class TestBooleanScorer extends LuceneTestCase {
     BooleanScorer bs = new BooleanScorer(weight, false, 1, Arrays.asList(scorers), Collections.<BulkScorer>emptyList(), scorers.length);
 
     final List<Integer> hits = new ArrayList<>();
    bs.score(new Collector() {
    bs.score(new SimpleCollector() {
       int docBase;
       @Override
       public void setScorer(Scorer scorer) {
@@ -111,7 +111,7 @@ public class TestBooleanScorer extends LuceneTestCase {
       }
       
       @Override
      public void setNextReader(AtomicReaderContext context) {
      protected void doSetNextReader(AtomicReaderContext context) throws IOException {
         docBase = context.docBase;
       }
       
@@ -149,7 +149,7 @@ public class TestBooleanScorer extends LuceneTestCase {
                             BooleanClause.Occur.SHOULD));
                             
     final int[] count = new int[1];
    s.search(q, new Collector() {
    s.search(q, new SimpleCollector() {
     
       @Override
       public void setScorer(Scorer scorer) {
@@ -163,10 +163,6 @@ public class TestBooleanScorer extends LuceneTestCase {
         count[0]++;
       }
       
      @Override
      public void setNextReader(AtomicReaderContext context) {
      }
      
       @Override
       public boolean acceptsDocsOutOfOrder() {
         return true;
@@ -219,7 +215,7 @@ public class TestBooleanScorer extends LuceneTestCase {
           return new BulkScorer() {
 
             @Override
            public boolean score(Collector collector, int max) throws IOException {
            public boolean score(LeafCollector collector, int max) throws IOException {
               collector.setScorer(new FakeScorer());
               collector.collect(0);
               return false;
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestCachingCollector.java b/lucene/core/src/test/org/apache/lucene/search/TestCachingCollector.java
index f493d42cdc3..e842909b3da 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestCachingCollector.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestCachingCollector.java
@@ -17,11 +17,10 @@ package org.apache.lucene.search;
  * limitations under the License.
  */
 
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.util.LuceneTestCase;

 import java.io.IOException;
 
import org.apache.lucene.util.LuceneTestCase;

 public class TestCachingCollector extends LuceneTestCase {
 
   private static final double ONE_BYTE = 1.0 / (1024 * 1024); // 1 byte out of MB
@@ -53,23 +52,17 @@ public class TestCachingCollector extends LuceneTestCase {
     } 
   }
   
  private static class NoOpCollector extends Collector {
  private static class NoOpCollector extends SimpleCollector {
 
     private final boolean acceptDocsOutOfOrder;
     
     public NoOpCollector(boolean acceptDocsOutOfOrder) {
       this.acceptDocsOutOfOrder = acceptDocsOutOfOrder;
     }
    
    @Override
    public void setScorer(Scorer scorer) throws IOException {}
 
     @Override
     public void collect(int doc) throws IOException {}
 
    @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {}

     @Override
     public boolean acceptsDocsOutOfOrder() {
       return acceptDocsOutOfOrder;
@@ -80,23 +73,18 @@ public class TestCachingCollector extends LuceneTestCase {
   public void testBasic() throws Exception {
     for (boolean cacheScores : new boolean[] { false, true }) {
       CachingCollector cc = CachingCollector.create(new NoOpCollector(false), cacheScores, 1.0);
      cc.setScorer(new MockScorer());
      LeafCollector acc = cc.getLeafCollector(null);
      acc.setScorer(new MockScorer());
 
       // collect 1000 docs
       for (int i = 0; i < 1000; i++) {
        cc.collect(i);
        acc.collect(i);
       }
 
       // now replay them
      cc.replay(new Collector() {
      cc.replay(new SimpleCollector() {
         int prevDocID = -1;
 
        @Override
        public void setScorer(Scorer scorer) {}

        @Override
        public void setNextReader(AtomicReaderContext context) {}

         @Override
         public void collect(int doc) {
           assertEquals(prevDocID + 1, doc);
@@ -113,11 +101,12 @@ public class TestCachingCollector extends LuceneTestCase {
   
   public void testIllegalStateOnReplay() throws Exception {
     CachingCollector cc = CachingCollector.create(new NoOpCollector(false), true, 50 * ONE_BYTE);
    cc.setScorer(new MockScorer());
    LeafCollector acc = cc.getLeafCollector(null);
    acc.setScorer(new MockScorer());
     
     // collect 130 docs, this should be enough for triggering cache abort.
     for (int i = 0; i < 130; i++) {
      cc.collect(i);
      acc.collect(i);
     }
     
     assertFalse("CachingCollector should not be cached due to low memory limit", cc.isCached());
@@ -135,16 +124,18 @@ public class TestCachingCollector extends LuceneTestCase {
     // is valid with the Collector passed to the ctor
     
     // 'src' Collector does not support out-of-order
    CachingCollector cc = CachingCollector.create(new NoOpCollector(false), true, 50 * ONE_BYTE);
    cc.setScorer(new MockScorer());
    for (int i = 0; i < 10; i++) cc.collect(i);
    CachingCollector cc = CachingCollector.create(new NoOpCollector(false), true, 100 * ONE_BYTE);
    LeafCollector acc = cc.getLeafCollector(null);
    acc.setScorer(new MockScorer());
    for (int i = 0; i < 10; i++) acc.collect(i);
     cc.replay(new NoOpCollector(true)); // this call should not fail
     cc.replay(new NoOpCollector(false)); // this call should not fail
 
     // 'src' Collector supports out-of-order
    cc = CachingCollector.create(new NoOpCollector(true), true, 50 * ONE_BYTE);
    cc.setScorer(new MockScorer());
    for (int i = 0; i < 10; i++) cc.collect(i);
    cc = CachingCollector.create(new NoOpCollector(true), true, 100 * ONE_BYTE);
    acc = cc.getLeafCollector(null);
    acc.setScorer(new MockScorer());
    for (int i = 0; i < 10; i++) acc.collect(i);
     cc.replay(new NoOpCollector(true)); // this call should not fail
     try {
       cc.replay(new NoOpCollector(false)); // this call should fail
@@ -165,12 +156,13 @@ public class TestCachingCollector extends LuceneTestCase {
       int bytesPerDoc = cacheScores ? 8 : 4;
       CachingCollector cc = CachingCollector.create(new NoOpCollector(false),
           cacheScores, bytesPerDoc * ONE_BYTE * numDocs);
      cc.setScorer(new MockScorer());
      for (int i = 0; i < numDocs; i++) cc.collect(i);
      LeafCollector acc = cc.getLeafCollector(null);
      acc.setScorer(new MockScorer());
      for (int i = 0; i < numDocs; i++) acc.collect(i);
       assertTrue(cc.isCached());
 
       // The 151's document should terminate caching
      cc.collect(numDocs);
      acc.collect(numDocs);
       assertFalse(cc.isCached());
     }
   }
@@ -179,9 +171,9 @@ public class TestCachingCollector extends LuceneTestCase {
     for (boolean cacheScores : new boolean[] { false, true }) {
       // create w/ null wrapped collector, and test that the methods work
       CachingCollector cc = CachingCollector.create(true, cacheScores, 50 * ONE_BYTE);
      cc.setNextReader(null);
      cc.setScorer(new MockScorer());
      cc.collect(0);
      LeafCollector acc = cc.getLeafCollector(null);
      acc.setScorer(new MockScorer());
      acc.collect(0);
       
       assertTrue(cc.isCached());
       cc.replay(new NoOpCollector(true));
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestConstantScoreQuery.java b/lucene/core/src/test/org/apache/lucene/search/TestConstantScoreQuery.java
index 21a6a7d4269..93d5a1da4ab 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestConstantScoreQuery.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestConstantScoreQuery.java
@@ -50,7 +50,7 @@ public class TestConstantScoreQuery extends LuceneTestCase {
   
   private void checkHits(IndexSearcher searcher, Query q, final float expectedScore, final String scorerClassName, final String innerScorerClassName) throws IOException {
     final int[] count = new int[1];
    searcher.search(q, new Collector() {
    searcher.search(q, new SimpleCollector() {
       private Scorer scorer;
     
       @Override
@@ -69,10 +69,6 @@ public class TestConstantScoreQuery extends LuceneTestCase {
         count[0]++;
       }
       
      @Override
      public void setNextReader(AtomicReaderContext context) {
      }
      
       @Override
       public boolean acceptsDocsOutOfOrder() {
         return true;
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestDocBoost.java b/lucene/core/src/test/org/apache/lucene/search/TestDocBoost.java
index 3cf2e85f83f..aced6f6f2d6 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestDocBoost.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestDocBoost.java
@@ -59,7 +59,7 @@ public class TestDocBoost extends LuceneTestCase {
     IndexSearcher searcher = newSearcher(reader);
     searcher.search
       (new TermQuery(new Term("field", "word")),
       new Collector() {
       new SimpleCollector() {
          private int base = 0;
          private Scorer scorer;
          @Override
@@ -71,7 +71,7 @@ public class TestDocBoost extends LuceneTestCase {
            scores[doc + base] = scorer.score();
          }
          @Override
         public void setNextReader(AtomicReaderContext context) {
         protected void doSetNextReader(AtomicReaderContext context) throws IOException {
            base = context.docBase;
          }
          @Override
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestEarlyTermination.java b/lucene/core/src/test/org/apache/lucene/search/TestEarlyTermination.java
index acbdaf7235b..7388d00f475 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestEarlyTermination.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestEarlyTermination.java
@@ -58,14 +58,11 @@ public class TestEarlyTermination extends LuceneTestCase {
 
     for (int i = 0; i < iters; ++i) {
       final IndexSearcher searcher = newSearcher(reader);
      final Collector collector = new Collector() {
      final Collector collector = new SimpleCollector() {
 
         final boolean outOfOrder = random().nextBoolean();
         boolean collectionTerminated = true;
 
        @Override
        public void setScorer(Scorer scorer) throws IOException {}

         @Override
         public void collect(int doc) throws IOException {
           assertFalse(collectionTerminated);
@@ -76,7 +73,7 @@ public class TestEarlyTermination extends LuceneTestCase {
         }
 
         @Override
        public void setNextReader(AtomicReaderContext context) throws IOException {
        protected void doSetNextReader(AtomicReaderContext context) throws IOException {
           if (random().nextBoolean()) {
             collectionTerminated = true;
             throw new CollectionTerminatedException();
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestMultiTermConstantScore.java b/lucene/core/src/test/org/apache/lucene/search/TestMultiTermConstantScore.java
index d286d5472a6..9f5f4381ed4 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestMultiTermConstantScore.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestMultiTermConstantScore.java
@@ -226,7 +226,7 @@ public class TestMultiTermConstantScore extends BaseTestRangeFilter {
     search.setSimilarity(new DefaultSimilarity());
     Query q = csrq("data", "1", "6", T, T);
     q.setBoost(100);
    search.search(q, null, new Collector() {
    search.search(q, null, new SimpleCollector() {
       private int base = 0;
       private Scorer scorer;
       @Override
@@ -238,7 +238,7 @@ public class TestMultiTermConstantScore extends BaseTestRangeFilter {
         assertEquals("score for doc " + (doc + base) + " was not correct", 1.0f, scorer.score(), SCORE_COMP_THRESH);
       }
       @Override
      public void setNextReader(AtomicReaderContext context) {
      protected void doSetNextReader(AtomicReaderContext context) throws IOException {
         base = context.docBase;
       }
       @Override
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestPositiveScoresOnlyCollector.java b/lucene/core/src/test/org/apache/lucene/search/TestPositiveScoresOnlyCollector.java
index 1f52b210697..4a51978c925 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestPositiveScoresOnlyCollector.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestPositiveScoresOnlyCollector.java
@@ -22,6 +22,7 @@ import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.store.Directory;
 import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.document.Document;
 
 public class TestPositiveScoresOnlyCollector extends LuceneTestCase {
 
@@ -78,6 +79,7 @@ public class TestPositiveScoresOnlyCollector extends LuceneTestCase {
     
     Directory directory = newDirectory();
     RandomIndexWriter writer = new RandomIndexWriter(random(), directory);
    writer.addDocument(new Document());
     writer.commit();
     IndexReader ir = writer.getReader();
     writer.close();
@@ -86,9 +88,10 @@ public class TestPositiveScoresOnlyCollector extends LuceneTestCase {
     Scorer s = new SimpleScorer(fake);
     TopDocsCollector<ScoreDoc> tdc = TopScoreDocCollector.create(scores.length, true);
     Collector c = new PositiveScoresOnlyCollector(tdc);
    c.setScorer(s);
    LeafCollector ac = c.getLeafCollector(ir.leaves().get(0));
    ac.setScorer(s);
     while (s.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
      c.collect(0);
      ac.collect(0);
     }
     TopDocs td = tdc.topDocs();
     ScoreDoc[] sd = td.scoreDocs;
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestScoreCachingWrappingScorer.java b/lucene/core/src/test/org/apache/lucene/search/TestScoreCachingWrappingScorer.java
index 9c6f4861ccf..c48c5f1bdbf 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestScoreCachingWrappingScorer.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestScoreCachingWrappingScorer.java
@@ -65,7 +65,7 @@ public class TestScoreCachingWrappingScorer extends LuceneTestCase {
     }
   }
   
  private static final class ScoreCachingCollector extends Collector {
  private static final class ScoreCachingCollector extends SimpleCollector {
 
     private int idx = 0;
     private Scorer scorer;
@@ -88,9 +88,6 @@ public class TestScoreCachingWrappingScorer extends LuceneTestCase {
       ++idx;
     }
 
    @Override public void setNextReader(AtomicReaderContext context) {
    }

     @Override public void setScorer(Scorer scorer) {
       this.scorer = new ScoreCachingWrappingScorer(scorer);
     }
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestScorerPerf.java b/lucene/core/src/test/org/apache/lucene/search/TestScorerPerf.java
index 48cbaee43b8..97dcc44c843 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestScorerPerf.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestScorerPerf.java
@@ -97,13 +97,10 @@ public class TestScorerPerf extends LuceneTestCase {
     return sets;
   }
 
  public static class CountingHitCollector extends Collector {
  public static class CountingHitCollector extends SimpleCollector {
     int count=0;
     int sum=0;
     protected int docBase = 0;

    @Override
    public void setScorer(Scorer scorer) throws IOException {}
     
     @Override
     public void collect(int doc) {
@@ -115,7 +112,7 @@ public class TestScorerPerf extends LuceneTestCase {
     public int getSum() { return sum; }
 
     @Override
    public void setNextReader(AtomicReaderContext context) {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       docBase = context.docBase;
     }
     @Override
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestSimilarity.java b/lucene/core/src/test/org/apache/lucene/search/TestSimilarity.java
index 31ebc14e2e2..3a3c58030a5 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestSimilarity.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestSimilarity.java
@@ -76,7 +76,7 @@ public class TestSimilarity extends LuceneTestCase {
     Term b = new Term("field", "b");
     Term c = new Term("field", "c");
 
    searcher.search(new TermQuery(b), new Collector() {
    searcher.search(new TermQuery(b), new SimpleCollector() {
          private Scorer scorer;
          @Override
         public void setScorer(Scorer scorer) {
@@ -86,9 +86,6 @@ public class TestSimilarity extends LuceneTestCase {
         public final void collect(int doc) throws IOException {
            assertEquals(1.0f, scorer.score(), 0);
          }
         @Override
        public void setNextReader(AtomicReaderContext context) {}
         @Override
         public boolean acceptsDocsOutOfOrder() {
            return true;
          }
@@ -98,7 +95,7 @@ public class TestSimilarity extends LuceneTestCase {
     bq.add(new TermQuery(a), BooleanClause.Occur.SHOULD);
     bq.add(new TermQuery(b), BooleanClause.Occur.SHOULD);
     //System.out.println(bq.toString("field"));
    searcher.search(bq, new Collector() {
    searcher.search(bq, new SimpleCollector() {
          private int base = 0;
          private Scorer scorer;
          @Override
@@ -111,7 +108,7 @@ public class TestSimilarity extends LuceneTestCase {
            assertEquals((float)doc+base+1, scorer.score(), 0);
          }
          @Override
        public void setNextReader(AtomicReaderContext context) {
         protected void doSetNextReader(AtomicReaderContext context) throws IOException {
            base = context.docBase;
          }
          @Override
@@ -125,7 +122,7 @@ public class TestSimilarity extends LuceneTestCase {
     pq.add(c);
     //System.out.println(pq.toString("field"));
     searcher.search(pq,
       new Collector() {
       new SimpleCollector() {
          private Scorer scorer;
          @Override
          public void setScorer(Scorer scorer) {
@@ -137,8 +134,6 @@ public class TestSimilarity extends LuceneTestCase {
            assertEquals(1.0f, scorer.score(), 0);
          }
          @Override
         public void setNextReader(AtomicReaderContext context) {}
         @Override
          public boolean acceptsDocsOutOfOrder() {
            return true;
          }
@@ -146,7 +141,7 @@ public class TestSimilarity extends LuceneTestCase {
 
     pq.setSlop(2);
     //System.out.println(pq.toString("field"));
    searcher.search(pq, new Collector() {
    searcher.search(pq, new SimpleCollector() {
       private Scorer scorer;
       @Override
       public void setScorer(Scorer scorer) {
@@ -158,8 +153,6 @@ public class TestSimilarity extends LuceneTestCase {
         assertEquals(2.0f, scorer.score(), 0);
       }
       @Override
      public void setNextReader(AtomicReaderContext context) {}
      @Override
       public boolean acceptsDocsOutOfOrder() {
         return true;
       }
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestSloppyPhraseQuery.java b/lucene/core/src/test/org/apache/lucene/search/TestSloppyPhraseQuery.java
index fdde5323713..85674196bb2 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestSloppyPhraseQuery.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestSloppyPhraseQuery.java
@@ -175,7 +175,7 @@ public class TestSloppyPhraseQuery extends LuceneTestCase {
     return query;
   }
 
  static class MaxFreqCollector extends Collector {
  static class MaxFreqCollector extends SimpleCollector {
     float max;
     int totalHits;
     Scorer scorer;
@@ -191,10 +191,6 @@ public class TestSloppyPhraseQuery extends LuceneTestCase {
       max = Math.max(max, scorer.freq());
     }
 
    @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {      
    }

     @Override
     public boolean acceptsDocsOutOfOrder() {
       return false;
@@ -203,7 +199,7 @@ public class TestSloppyPhraseQuery extends LuceneTestCase {
   
   /** checks that no scores or freqs are infinite */
   private void assertSaneScoring(PhraseQuery pq, IndexSearcher searcher) throws Exception {
    searcher.search(pq, new Collector() {
    searcher.search(pq, new SimpleCollector() {
       Scorer scorer;
       
       @Override
@@ -217,11 +213,6 @@ public class TestSloppyPhraseQuery extends LuceneTestCase {
         assertFalse(Float.isInfinite(scorer.score()));
       }
       
      @Override
      public void setNextReader(AtomicReaderContext context) {
        // do nothing
      }
      
       @Override
       public boolean acceptsDocsOutOfOrder() {
         return false;
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestSubScorerFreqs.java b/lucene/core/src/test/org/apache/lucene/search/TestSubScorerFreqs.java
index 063d26b5c40..def8988ae66 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestSubScorerFreqs.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestSubScorerFreqs.java
@@ -65,10 +65,7 @@ public class TestSubScorerFreqs extends LuceneTestCase {
     dir = null;
   }
 
  private static class CountingCollector extends Collector {
    private final Collector other;
    private int docBase;

  private static class CountingCollector extends FilterCollector {
     public final Map<Integer, Map<Query, Float>> docCounts = new HashMap<>();
 
     private final Map<Query, Scorer> subScorers = new HashMap<>();
@@ -79,16 +76,9 @@ public class TestSubScorerFreqs extends LuceneTestCase {
     }
 
     public CountingCollector(Collector other, Set<String> relationships) {
      this.other = other;
      super(other);
       this.relationships = relationships;
     }

    @Override
    public void setScorer(Scorer scorer) throws IOException {
      other.setScorer(scorer);
      subScorers.clear();
      setSubScorers(scorer, "TOP");
    }
     
     public void setSubScorers(Scorer scorer, String relationship) {
       for (ChildScorer child : scorer.getChildren()) {
@@ -98,30 +88,34 @@ public class TestSubScorerFreqs extends LuceneTestCase {
       }
       subScorers.put(scorer.getWeight().getQuery(), scorer);
     }

    @Override
    public void collect(int doc) throws IOException {
      final Map<Query, Float> freqs = new HashMap<>();
      for (Map.Entry<Query, Scorer> ent : subScorers.entrySet()) {
        Scorer value = ent.getValue();
        int matchId = value.docID();
        freqs.put(ent.getKey(), matchId == doc ? value.freq() : 0.0f);
      }
      docCounts.put(doc + docBase, freqs);
      other.collect(doc);
    }

    @Override
    public void setNextReader(AtomicReaderContext context)
    
    public LeafCollector getLeafCollector(AtomicReaderContext context)
         throws IOException {
      docBase = context.docBase;
      other.setNextReader(context);
      final int docBase = context.docBase;
      return new FilterLeafCollector(super.getLeafCollector(context)) {
        
        @Override
        public void collect(int doc) throws IOException {
          final Map<Query, Float> freqs = new HashMap<Query, Float>();
          for (Map.Entry<Query, Scorer> ent : subScorers.entrySet()) {
            Scorer value = ent.getValue();
            int matchId = value.docID();
            freqs.put(ent.getKey(), matchId == doc ? value.freq() : 0.0f);
          }
          docCounts.put(doc + docBase, freqs);
          super.collect(doc);
        }
        
        @Override
        public void setScorer(Scorer scorer) throws IOException {
          super.setScorer(scorer);
          subScorers.clear();
          setSubScorers(scorer, "TOP");
        }
        
      };
     }
 
    @Override
    public boolean acceptsDocsOutOfOrder() {
      return other.acceptsDocsOutOfOrder();
    }
   }
 
   private static final float FLOAT_TOLERANCE = 0.00001F;
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestTermScorer.java b/lucene/core/src/test/org/apache/lucene/search/TestTermScorer.java
index 6b4f474f8d4..44faa83a892 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestTermScorer.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestTermScorer.java
@@ -84,7 +84,7 @@ public class TestTermScorer extends LuceneTestCase {
     final List<TestHit> docs = new ArrayList<>();
     // must call next first
     
    ts.score(new Collector() {
    ts.score(new SimpleCollector() {
       private int base = 0;
       private Scorer scorer;
       
@@ -104,7 +104,7 @@ public class TestTermScorer extends LuceneTestCase {
       }
       
       @Override
      public void setNextReader(AtomicReaderContext context) {
      protected void doSetNextReader(AtomicReaderContext context) throws IOException {
         base = context.docBase;
       }
       
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestTimeLimitingCollector.java b/lucene/core/src/test/org/apache/lucene/search/TestTimeLimitingCollector.java
index 537e30f9176..85239c62b10 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestTimeLimitingCollector.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestTimeLimitingCollector.java
@@ -307,7 +307,7 @@ public class TestTimeLimitingCollector extends LuceneTestCase {
   }
   
   // counting collector that can slow down at collect().
  private class MyHitCollector extends Collector {
  private class MyHitCollector extends SimpleCollector {
     private final BitSet bits = new BitSet();
     private int slowdown = 0;
     private int lastDocCollected = -1;
@@ -349,7 +349,7 @@ public class TestTimeLimitingCollector extends LuceneTestCase {
     }
     
     @Override
    public void setNextReader(AtomicReaderContext context) {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       docBase = context.docBase;
     }
     
diff --git a/lucene/core/src/test/org/apache/lucene/search/TestTopDocsCollector.java b/lucene/core/src/test/org/apache/lucene/search/TestTopDocsCollector.java
index f8f728f9061..0c56e111cc2 100644
-- a/lucene/core/src/test/org/apache/lucene/search/TestTopDocsCollector.java
++ b/lucene/core/src/test/org/apache/lucene/search/TestTopDocsCollector.java
@@ -61,7 +61,7 @@ public class TestTopDocsCollector extends LuceneTestCase {
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       base = context.docBase;
     }
 
diff --git a/lucene/facet/src/java/org/apache/lucene/facet/DrillSideways.java b/lucene/facet/src/java/org/apache/lucene/facet/DrillSideways.java
index 01b3b6c5b53..c8614f3fa53 100644
-- a/lucene/facet/src/java/org/apache/lucene/facet/DrillSideways.java
++ b/lucene/facet/src/java/org/apache/lucene/facet/DrillSideways.java
@@ -26,6 +26,7 @@ import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
 import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
 import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
 import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.Collector;
@@ -238,7 +239,7 @@ public class DrillSideways {
    *  default is false.  Note that if you return true from
    *  this method (in a subclass) be sure your collector
    *  also returns false from {@link
   *  Collector#acceptsDocsOutOfOrder}: this will trick
   *  LeafCollector#acceptsDocsOutOfOrder}: this will trick
    *  {@code BooleanQuery} into also scoring all subDocs at
    *  once. */
   protected boolean scoreSubDocsAtOnce() {
diff --git a/lucene/facet/src/java/org/apache/lucene/facet/DrillSidewaysScorer.java b/lucene/facet/src/java/org/apache/lucene/facet/DrillSidewaysScorer.java
index 1d08bf77d9a..273b6b18569 100644
-- a/lucene/facet/src/java/org/apache/lucene/facet/DrillSidewaysScorer.java
++ b/lucene/facet/src/java/org/apache/lucene/facet/DrillSidewaysScorer.java
@@ -23,6 +23,7 @@ import java.util.Collections;
 
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.DocIdSetIterator;
 import org.apache.lucene.search.Scorer;
@@ -36,6 +37,7 @@ class DrillSidewaysScorer extends BulkScorer {
   //private static boolean DEBUG = false;
 
   private final Collector drillDownCollector;
  private LeafCollector drillDownLeafCollector;
 
   private final DocsAndCost[] dims;
 
@@ -62,7 +64,7 @@ class DrillSidewaysScorer extends BulkScorer {
   }
 
   @Override
  public boolean score(Collector collector, int maxDoc) throws IOException {
  public boolean score(LeafCollector collector, int maxDoc) throws IOException {
     if (maxDoc != Integer.MAX_VALUE) {
       throw new IllegalArgumentException("maxDoc must be Integer.MAX_VALUE");
     }
@@ -73,12 +75,14 @@ class DrillSidewaysScorer extends BulkScorer {
     FakeScorer scorer = new FakeScorer();
     collector.setScorer(scorer);
     if (drillDownCollector != null) {
      drillDownCollector.setScorer(scorer);
      drillDownCollector.setNextReader(context);
      drillDownLeafCollector = drillDownCollector.getLeafCollector(context);
      drillDownLeafCollector.setScorer(scorer);
    } else {
      drillDownLeafCollector = null;
     }
     for (DocsAndCost dim : dims) {
      dim.sidewaysCollector.setScorer(scorer);
      dim.sidewaysCollector.setNextReader(context);
      dim.sidewaysLeafCollector = dim.sidewaysCollector.getLeafCollector(context);
      dim.sidewaysLeafCollector.setScorer(scorer);
     }
 
     // TODO: if we ever allow null baseScorer ... it will
@@ -100,10 +104,10 @@ class DrillSidewaysScorer extends BulkScorer {
     final int numDims = dims.length;
 
     Bits[] bits = new Bits[numBits];
    Collector[] bitsSidewaysCollectors = new Collector[numBits];
    LeafCollector[] bitsSidewaysCollectors = new LeafCollector[numBits];
 
     DocIdSetIterator[] disis = new DocIdSetIterator[numDims-numBits];
    Collector[] sidewaysCollectors = new Collector[numDims-numBits];
    LeafCollector[] sidewaysCollectors = new LeafCollector[numDims-numBits];
     long drillDownCost = 0;
     int disiUpto = 0;
     int bitsUpto = 0;
@@ -111,14 +115,14 @@ class DrillSidewaysScorer extends BulkScorer {
       DocIdSetIterator disi = dims[dim].disi;
       if (dims[dim].bits == null) {
         disis[disiUpto] = disi;
        sidewaysCollectors[disiUpto] = dims[dim].sidewaysCollector;
        sidewaysCollectors[disiUpto] = dims[dim].sidewaysLeafCollector;
         disiUpto++;
         if (disi != null) {
           drillDownCost += disi.cost();
         }
       } else {
         bits[bitsUpto] = dims[dim].bits;
        bitsSidewaysCollectors[bitsUpto] = dims[dim].sidewaysCollector;
        bitsSidewaysCollectors[bitsUpto] = dims[dim].sidewaysLeafCollector;
         bitsUpto++;
       }
     }
@@ -154,15 +158,15 @@ class DrillSidewaysScorer extends BulkScorer {
    *  (i.e., like BooleanScorer2, not BooleanScorer).  In
    *  this case we just .next() on base and .advance() on
    *  the dim filters. */ 
  private void doQueryFirstScoring(Collector collector, DocIdSetIterator[] disis, Collector[] sidewaysCollectors,
                                   Bits[] bits, Collector[] bitsSidewaysCollectors) throws IOException {
  private void doQueryFirstScoring(LeafCollector collector, DocIdSetIterator[] disis, LeafCollector[] sidewaysCollectors,
                                   Bits[] bits, LeafCollector[] bitsSidewaysCollectors) throws IOException {
     //if (DEBUG) {
     //  System.out.println("  doQueryFirstScoring");
     //}
     int docID = baseScorer.docID();
 
     nextDoc: while (docID != DocsEnum.NO_MORE_DOCS) {
      Collector failedCollector = null;
      LeafCollector failedCollector = null;
       for (int i=0;i<disis.length;i++) {
         // TODO: should we sort this 2nd dimension of
         // docsEnums from most frequent to least?
@@ -225,7 +229,7 @@ class DrillSidewaysScorer extends BulkScorer {
 
   /** Used when drill downs are highly constraining vs
    *  baseQuery. */
  private void doDrillDownAdvanceScoring(Collector collector, DocIdSetIterator[] disis, Collector[] sidewaysCollectors) throws IOException {
  private void doDrillDownAdvanceScoring(LeafCollector collector, DocIdSetIterator[] disis, LeafCollector[] sidewaysCollectors) throws IOException {
     final int maxDoc = context.reader().maxDoc();
     final int numDims = dims.length;
 
@@ -423,7 +427,7 @@ class DrillSidewaysScorer extends BulkScorer {
     }
   }
 
  private void doUnionScoring(Collector collector, DocIdSetIterator[] disis, Collector[] sidewaysCollectors) throws IOException {
  private void doUnionScoring(LeafCollector collector, DocIdSetIterator[] disis, LeafCollector[] sidewaysCollectors) throws IOException {
     //if (DEBUG) {
     //  System.out.println("  doUnionScoring");
     //}
@@ -569,14 +573,14 @@ class DrillSidewaysScorer extends BulkScorer {
     }
   }
 
  private void collectHit(Collector collector, Collector[] sidewaysCollectors) throws IOException {
  private void collectHit(LeafCollector collector, LeafCollector[] sidewaysCollectors) throws IOException {
     //if (DEBUG) {
     //  System.out.println("      hit");
     //}
 
     collector.collect(collectDocID);
     if (drillDownCollector != null) {
      drillDownCollector.collect(collectDocID);
      drillDownLeafCollector.collect(collectDocID);
     }
 
     // TODO: we could "fix" faceting of the sideways counts
@@ -589,14 +593,14 @@ class DrillSidewaysScorer extends BulkScorer {
     }
   }
 
  private void collectHit(Collector collector, Collector[] sidewaysCollectors, Collector[] sidewaysCollectors2) throws IOException {
  private void collectHit(LeafCollector collector, LeafCollector[] sidewaysCollectors, LeafCollector[] sidewaysCollectors2) throws IOException {
     //if (DEBUG) {
     //  System.out.println("      hit");
     //}
 
     collector.collect(collectDocID);
     if (drillDownCollector != null) {
      drillDownCollector.collect(collectDocID);
      drillDownLeafCollector.collect(collectDocID);
     }
 
     // TODO: we could "fix" faceting of the sideways counts
@@ -612,7 +616,7 @@ class DrillSidewaysScorer extends BulkScorer {
     }
   }
 
  private void collectNearMiss(Collector sidewaysCollector) throws IOException {
  private void collectNearMiss(LeafCollector sidewaysCollector) throws IOException {
     //if (DEBUG) {
     //  System.out.println("      missingDim=" + dim);
     //}
@@ -620,8 +624,6 @@ class DrillSidewaysScorer extends BulkScorer {
   }
 
   private final class FakeScorer extends Scorer {
    float score;
    int doc;
 
     public FakeScorer() {
       super(null);
@@ -674,6 +676,7 @@ class DrillSidewaysScorer extends BulkScorer {
     // Random access bits:
     Bits bits;
     Collector sidewaysCollector;
    LeafCollector sidewaysLeafCollector;
     String dim;
 
     @Override
diff --git a/lucene/facet/src/java/org/apache/lucene/facet/FacetsCollector.java b/lucene/facet/src/java/org/apache/lucene/facet/FacetsCollector.java
index 912725d4c65..90bbba6320d 100644
-- a/lucene/facet/src/java/org/apache/lucene/facet/FacetsCollector.java
++ b/lucene/facet/src/java/org/apache/lucene/facet/FacetsCollector.java
@@ -32,6 +32,7 @@ import org.apache.lucene.search.MultiCollector;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.ScoreDoc;
 import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
 import org.apache.lucene.search.Sort;
 import org.apache.lucene.search.TopDocs;
 import org.apache.lucene.search.TopDocsCollector;
@@ -47,7 +48,7 @@ import org.apache.lucene.util.FixedBitSet;
  *  counting.  Use the {@code search} utility methods to
  *  perform an "ordinary" search but also collect into a
  *  {@link Collector}. */
public class FacetsCollector extends Collector {
public class FacetsCollector extends SimpleCollector {
 
   private AtomicReaderContext context;
   private Scorer scorer;
@@ -151,7 +152,7 @@ public class FacetsCollector extends Collector {
 
     return matchingDocs;
   }
    

   @Override
   public final boolean acceptsDocsOutOfOrder() {
     // If we are keeping scores then we require in-order
@@ -180,7 +181,7 @@ public class FacetsCollector extends Collector {
   }
     
   @Override
  public final void setNextReader(AtomicReaderContext context) throws IOException {
  protected void doSetNextReader(AtomicReaderContext context) throws IOException {
     if (docs != null) {
       matchingDocs.add(new MatchingDocs(this.context, docs.getDocIdSet(), totalHits, scores));
     }
diff --git a/lucene/facet/src/test/org/apache/lucene/facet/AssertingSubDocsAtOnceCollector.java b/lucene/facet/src/test/org/apache/lucene/facet/AssertingSubDocsAtOnceCollector.java
index 644b3adc8e0..df104b0793e 100644
-- a/lucene/facet/src/test/org/apache/lucene/facet/AssertingSubDocsAtOnceCollector.java
++ b/lucene/facet/src/test/org/apache/lucene/facet/AssertingSubDocsAtOnceCollector.java
@@ -20,14 +20,13 @@ package org.apache.lucene.facet;
 import java.util.ArrayList;
 import java.util.List;
 
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer.ChildScorer;
 import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Scorer.ChildScorer;
import org.apache.lucene.search.SimpleCollector;
 
 /** Verifies in collect() that all child subScorers are on
  *  the collected doc. */
class AssertingSubDocsAtOnceCollector extends Collector {
class AssertingSubDocsAtOnceCollector extends SimpleCollector {
 
   // TODO: allow wrapping another Collector
 
@@ -56,10 +55,6 @@ class AssertingSubDocsAtOnceCollector extends Collector {
     }
   }
 
  @Override
  public void setNextReader(AtomicReaderContext context) {
  }

   @Override
   public boolean acceptsDocsOutOfOrder() {
     return false;
diff --git a/lucene/facet/src/test/org/apache/lucene/facet/TestDrillSideways.java b/lucene/facet/src/test/org/apache/lucene/facet/TestDrillSideways.java
index a6875cba714..6847da75680 100644
-- a/lucene/facet/src/test/org/apache/lucene/facet/TestDrillSideways.java
++ b/lucene/facet/src/test/org/apache/lucene/facet/TestDrillSideways.java
@@ -43,6 +43,7 @@ import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.DocIdSet;
 import org.apache.lucene.search.Filter;
@@ -51,6 +52,7 @@ import org.apache.lucene.search.MatchAllDocsQuery;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.ScoreDoc;
 import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
 import org.apache.lucene.search.Sort;
 import org.apache.lucene.search.SortField;
 import org.apache.lucene.search.TermQuery;
@@ -666,13 +668,9 @@ public class TestDrillSideways extends FacetTestCase {
       // had an AssertingScorer it could catch it when
       // Weight.scoresDocsOutOfOrder lies!:
       new DrillSideways(s, config, tr).search(ddq,
                           new Collector() {
                           new SimpleCollector() {
                              int lastDocID;
 
                             @Override
                             public void setScorer(Scorer s) {
                             }

                              @Override
                              public void collect(int doc) {
                                assert doc > lastDocID;
@@ -680,7 +678,7 @@ public class TestDrillSideways extends FacetTestCase {
                              }
 
                              @Override
                             public void setNextReader(AtomicReaderContext context) {
                             protected void doSetNextReader(AtomicReaderContext context) throws IOException {
                                lastDocID = -1;
                              }
 
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractAllGroupHeadsCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractAllGroupHeadsCollector.java
index 31abf0bc3a3..be6d8f8b92e 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractAllGroupHeadsCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractAllGroupHeadsCollector.java
@@ -17,20 +17,20 @@ package org.apache.lucene.search.grouping;
  * limitations under the License.
  */
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.util.FixedBitSet;

 import java.io.IOException;
 import java.util.Collection;
 
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.util.FixedBitSet;

 /**
  * This collector specializes in collecting the most relevant document (group head) for each group that match the query.
  *
  * @lucene.experimental
  */
 @SuppressWarnings({"unchecked","rawtypes"})
public abstract class AbstractAllGroupHeadsCollector<GH extends AbstractAllGroupHeadsCollector.GroupHead> extends Collector {
public abstract class AbstractAllGroupHeadsCollector<GH extends AbstractAllGroupHeadsCollector.GroupHead> extends SimpleCollector {
 
   protected final int[] reversed;
   protected final int compIDXEnd;
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractAllGroupsCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractAllGroupsCollector.java
index 3cd9164df17..1677ecaa5fa 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractAllGroupsCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractAllGroupsCollector.java
@@ -17,13 +17,13 @@ package org.apache.lucene.search.grouping;
  * limitations under the License.
  */
 
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.util.BytesRef;

 import java.io.IOException;
 import java.util.Collection;
 
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.util.BytesRef;

 /**
  * A collector that collects all groups that match the
  * query. Only the group value is collected, and the order
@@ -36,7 +36,7 @@ import java.util.Collection;
  *
  * @lucene.experimental
  */
public abstract class AbstractAllGroupsCollector<GROUP_VALUE_TYPE> extends Collector {
public abstract class AbstractAllGroupsCollector<GROUP_VALUE_TYPE> extends SimpleCollector {
 
   /**
    * Returns the total number of groups for the executed search.
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractDistinctValuesCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractDistinctValuesCollector.java
index 07fc35e0aa5..a735caf48e9 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractDistinctValuesCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractDistinctValuesCollector.java
@@ -17,18 +17,18 @@ package org.apache.lucene.search.grouping;
  * limitations under the License.
  */
 
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
 
import java.io.IOException;
import java.util.*;
import org.apache.lucene.search.SimpleCollector;
 
 /**
  * A second pass grouping collector that keeps track of distinct values for a specified field for the top N group.
  *
  * @lucene.experimental
  */
public abstract class AbstractDistinctValuesCollector<GC extends AbstractDistinctValuesCollector.GroupCount<?>> extends Collector {
public abstract class AbstractDistinctValuesCollector<GC extends AbstractDistinctValuesCollector.GroupCount<?>> extends SimpleCollector {
 
   /**
    * Returns all unique values for each top N group.
@@ -42,10 +42,6 @@ public abstract class AbstractDistinctValuesCollector<GC extends AbstractDistinc
     return true;
   }
 
  @Override
  public void setScorer(Scorer scorer) throws IOException {
  }

   /**
    * Returned by {@link AbstractDistinctValuesCollector#getGroups()},
    * representing the value and set of distinct values for the group.
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractFirstPassGroupingCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractFirstPassGroupingCollector.java
index 19b1d36052e..0c342ecddae 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractFirstPassGroupingCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractFirstPassGroupingCollector.java
@@ -33,7 +33,7 @@ import java.util.*;
  *
  * @lucene.experimental
  */
abstract public class AbstractFirstPassGroupingCollector<GROUP_VALUE_TYPE> extends Collector {
abstract public class AbstractFirstPassGroupingCollector<GROUP_VALUE_TYPE> extends SimpleCollector {
 
   private final Sort groupSort;
   private final FieldComparator<?>[] comparators;
@@ -326,7 +326,7 @@ abstract public class AbstractFirstPassGroupingCollector<GROUP_VALUE_TYPE> exten
   }
 
   @Override
  public void setNextReader(AtomicReaderContext readerContext) throws IOException {
  protected void doSetNextReader(AtomicReaderContext readerContext) throws IOException {
     docBase = readerContext.docBase;
     for (int i=0; i<comparators.length; i++) {
       comparators[i] = comparators[i].setNextReader(readerContext);
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractGroupFacetCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractGroupFacetCollector.java
index 8db044c8c1f..016f393c908 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractGroupFacetCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractGroupFacetCollector.java
@@ -19,6 +19,7 @@ package org.apache.lucene.search.grouping;
 
 import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.PriorityQueue;
 
@@ -30,7 +31,7 @@ import java.util.*;
  *
  * @lucene.experimental
  */
public abstract class AbstractGroupFacetCollector extends Collector {
public abstract class AbstractGroupFacetCollector extends SimpleCollector {
 
   protected final String groupField;
   protected final String facetField;
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractSecondPassGroupingCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractSecondPassGroupingCollector.java
index 7b000124e07..aedfa9ea21c 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractSecondPassGroupingCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/AbstractSecondPassGroupingCollector.java
@@ -37,7 +37,7 @@ import java.util.Map;
  *
  * @lucene.experimental
  */
public abstract class AbstractSecondPassGroupingCollector<GROUP_VALUE_TYPE> extends Collector {
public abstract class AbstractSecondPassGroupingCollector<GROUP_VALUE_TYPE> extends SimpleCollector {
 
   protected final Map<GROUP_VALUE_TYPE, SearchGroupDocs<GROUP_VALUE_TYPE>> groupMap;
   private final int maxDocsPerGroup;
@@ -107,10 +107,10 @@ public abstract class AbstractSecondPassGroupingCollector<GROUP_VALUE_TYPE> exte
   protected abstract SearchGroupDocs<GROUP_VALUE_TYPE> retrieveGroup(int doc) throws IOException;
 
   @Override
  public void setNextReader(AtomicReaderContext readerContext) throws IOException {
  protected void doSetNextReader(AtomicReaderContext readerContext) throws IOException {
     //System.out.println("SP.setNextReader");
     for (SearchGroupDocs<GROUP_VALUE_TYPE> group : groupMap.values()) {
      group.collector.setNextReader(readerContext);
      group.collector.getLeafCollector(readerContext);
     }
   }
 
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/BlockGroupingCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/BlockGroupingCollector.java
index 195ec230ed7..7c33583292f 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/BlockGroupingCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/BlockGroupingCollector.java
@@ -55,7 +55,7 @@ import org.apache.lucene.util.PriorityQueue;
  * @lucene.experimental
  */
 
public class BlockGroupingCollector extends Collector {
public class BlockGroupingCollector extends SimpleCollector {
 
   private int[] pendingSubDocs;
   private float[] pendingSubScores;
@@ -350,7 +350,7 @@ public class BlockGroupingCollector extends Collector {
       }
 
       collector.setScorer(fakeScorer);
      collector.setNextReader(og.readerContext);
      collector.getLeafCollector(og.readerContext);
       for(int docIDX=0;docIDX<og.count;docIDX++) {
         final int doc = og.docs[docIDX];
         fakeScorer.doc = doc;
@@ -516,7 +516,7 @@ public class BlockGroupingCollector extends Collector {
   }
 
   @Override
  public void setNextReader(AtomicReaderContext readerContext) throws IOException {
  protected void doSetNextReader(AtomicReaderContext readerContext) throws IOException {
     if (subDocUpto != 0) {
       processGroup();
     }
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionAllGroupHeadsCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionAllGroupHeadsCollector.java
index 8372ac5bc61..64ad845a517 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionAllGroupHeadsCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionAllGroupHeadsCollector.java
@@ -20,6 +20,7 @@ package org.apache.lucene.search.grouping.function;
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.queries.function.FunctionValues;
 import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.FieldComparator;
 import org.apache.lucene.search.Scorer;
 import org.apache.lucene.search.Sort;
@@ -101,7 +102,7 @@ public class FunctionAllGroupHeadsCollector extends AbstractAllGroupHeadsCollect
   }
 
   @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
  protected void doSetNextReader(AtomicReaderContext context) throws IOException {
     this.readerContext = context;
     FunctionValues values = groupBy.getValues(vsContext, context);
     filler = values.getValueFiller();
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionAllGroupsCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionAllGroupsCollector.java
index c778162e0ca..d949bec7bfd 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionAllGroupsCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionAllGroupsCollector.java
@@ -20,6 +20,7 @@ package org.apache.lucene.search.grouping.function;
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.queries.function.FunctionValues;
 import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.grouping.AbstractAllGroupsCollector;
 import org.apache.lucene.util.mutable.MutableValue;
 
@@ -75,7 +76,7 @@ public class FunctionAllGroupsCollector extends AbstractAllGroupsCollector<Mutab
   }
 
   @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
  protected void doSetNextReader(AtomicReaderContext context) throws IOException {
     FunctionValues values = groupBy.getValues(vsContext, context);
     filler = values.getValueFiller();
     mval = filler.getValue();
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionDistinctValuesCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionDistinctValuesCollector.java
index 3bc707482fc..597a1966796 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionDistinctValuesCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionDistinctValuesCollector.java
@@ -20,6 +20,7 @@ package org.apache.lucene.search.grouping.function;
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.queries.function.FunctionValues;
 import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.grouping.AbstractDistinctValuesCollector;
 import org.apache.lucene.search.grouping.SearchGroup;
 import org.apache.lucene.util.mutable.MutableValue;
@@ -70,7 +71,7 @@ public class FunctionDistinctValuesCollector extends AbstractDistinctValuesColle
   }
 
   @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
  protected void doSetNextReader(AtomicReaderContext context) throws IOException {
     FunctionValues values = groupSource.getValues(vsContext, context);
     groupFiller = values.getValueFiller();
     groupMval = groupFiller.getValue();
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionFirstPassGroupingCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionFirstPassGroupingCollector.java
index 6355a516218..b9737e1b9b7 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionFirstPassGroupingCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionFirstPassGroupingCollector.java
@@ -20,6 +20,7 @@ package org.apache.lucene.search.grouping.function;
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.queries.function.FunctionValues;
 import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.Sort;
 import org.apache.lucene.search.grouping.AbstractFirstPassGroupingCollector;
 import org.apache.lucene.util.mutable.MutableValue;
@@ -77,8 +78,8 @@ public class FunctionFirstPassGroupingCollector extends AbstractFirstPassGroupin
   }
 
   @Override
  public void setNextReader(AtomicReaderContext readerContext) throws IOException {
    super.setNextReader(readerContext);
  protected void doSetNextReader(AtomicReaderContext readerContext) throws IOException {
    super.doSetNextReader(readerContext);
     FunctionValues values = groupByVS.getValues(vsContext, readerContext);
     filler = values.getValueFiller();
     mval = filler.getValue();
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionSecondPassGroupingCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionSecondPassGroupingCollector.java
index d2f1d597a01..9df094be793 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionSecondPassGroupingCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/function/FunctionSecondPassGroupingCollector.java
@@ -20,6 +20,7 @@ package org.apache.lucene.search.grouping.function;
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.queries.function.FunctionValues;
 import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.Sort;
 import org.apache.lucene.search.grouping.AbstractSecondPassGroupingCollector;
 import org.apache.lucene.search.grouping.SearchGroup;
@@ -71,8 +72,8 @@ public class FunctionSecondPassGroupingCollector extends AbstractSecondPassGroup
   }
 
   @Override
  public void setNextReader(AtomicReaderContext readerContext) throws IOException {
    super.setNextReader(readerContext);
  protected void doSetNextReader(AtomicReaderContext readerContext) throws IOException {
    super.doSetNextReader(readerContext);
     FunctionValues values = groupByVS.getValues(vsContext, readerContext);
     filler = values.getValueFiller();
     mval = filler.getValue();
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermAllGroupHeadsCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermAllGroupHeadsCollector.java
index 2367f4d6387..45192c11c58 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermAllGroupHeadsCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermAllGroupHeadsCollector.java
@@ -19,6 +19,7 @@ package org.apache.lucene.search.grouping.term;
 
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.FieldCache;
 import org.apache.lucene.search.FieldComparator;
 import org.apache.lucene.search.Scorer;
@@ -158,7 +159,7 @@ public abstract class TermAllGroupHeadsCollector<GH extends AbstractAllGroupHead
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       this.readerContext = context;
       groupIndex = FieldCache.DEFAULT.getTermsIndex(context.reader(), groupField);
 
@@ -273,7 +274,7 @@ public abstract class TermAllGroupHeadsCollector<GH extends AbstractAllGroupHead
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       this.readerContext = context;
       groupIndex = FieldCache.DEFAULT.getTermsIndex(context.reader(), groupField);
       for (int i = 0; i < fields.length; i++) {
@@ -441,7 +442,7 @@ public abstract class TermAllGroupHeadsCollector<GH extends AbstractAllGroupHead
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       this.readerContext = context;
       groupIndex = FieldCache.DEFAULT.getTermsIndex(context.reader(), groupField);
       for (int i = 0; i < fields.length; i++) {
@@ -584,7 +585,7 @@ public abstract class TermAllGroupHeadsCollector<GH extends AbstractAllGroupHead
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       this.readerContext = context;
       groupIndex = FieldCache.DEFAULT.getTermsIndex(context.reader(), groupField);
 
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermAllGroupsCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermAllGroupsCollector.java
index 548640416c9..0ff1e57c539 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermAllGroupsCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermAllGroupsCollector.java
@@ -19,6 +19,7 @@ package org.apache.lucene.search.grouping.term;
 
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.FieldCache;
 import org.apache.lucene.search.grouping.AbstractAllGroupsCollector;
 import org.apache.lucene.util.BytesRef;
@@ -103,7 +104,7 @@ public class TermAllGroupsCollector extends AbstractAllGroupsCollector<BytesRef>
   }
 
   @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
  protected void doSetNextReader(AtomicReaderContext context) throws IOException {
     index = FieldCache.DEFAULT.getTermsIndex(context.reader(), groupField);
 
     // Clear ordSet and fill it with previous encountered groups that can occur in the current segment.
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermDistinctValuesCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermDistinctValuesCollector.java
index 7dad9f01173..c718dc2b4d6 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermDistinctValuesCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermDistinctValuesCollector.java
@@ -19,6 +19,7 @@ package org.apache.lucene.search.grouping.term;
 
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.FieldCache;
 import org.apache.lucene.search.grouping.AbstractDistinctValuesCollector;
 import org.apache.lucene.search.grouping.SearchGroup;
@@ -107,7 +108,7 @@ public class TermDistinctValuesCollector extends AbstractDistinctValuesCollector
   }
 
   @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
  protected void doSetNextReader(AtomicReaderContext context) throws IOException {
     groupFieldTermIndex = FieldCache.DEFAULT.getTermsIndex(context.reader(), groupField);
     countFieldTermIndex = FieldCache.DEFAULT.getTermsIndex(context.reader(), countField);
     ordSet.clear();
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermFirstPassGroupingCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermFirstPassGroupingCollector.java
index 70b71b8111e..6c708a924eb 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermFirstPassGroupingCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermFirstPassGroupingCollector.java
@@ -21,6 +21,7 @@ import java.io.IOException;
 
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.FieldCache;
 import org.apache.lucene.search.Sort;
 import org.apache.lucene.search.grouping.AbstractFirstPassGroupingCollector;
@@ -85,8 +86,8 @@ public class TermFirstPassGroupingCollector extends AbstractFirstPassGroupingCol
   }
 
   @Override
  public void setNextReader(AtomicReaderContext readerContext) throws IOException {
    super.setNextReader(readerContext);
  protected void doSetNextReader(AtomicReaderContext readerContext) throws IOException {
    super.doSetNextReader(readerContext);
     index = FieldCache.DEFAULT.getTermsIndex(readerContext.reader(), groupField);
   }
 }
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermGroupFacetCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermGroupFacetCollector.java
index 075214af221..5cff4b0d8fd 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermGroupFacetCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermGroupFacetCollector.java
@@ -21,6 +21,7 @@ import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.SortedDocValues;
 import org.apache.lucene.index.SortedSetDocValues;
 import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.FieldCache;
 import org.apache.lucene.search.grouping.AbstractGroupFacetCollector;
 import org.apache.lucene.util.BytesRef;
@@ -122,7 +123,7 @@ public abstract class TermGroupFacetCollector extends AbstractGroupFacetCollecto
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       if (segmentFacetCounts != null) {
         segmentResults.add(createSegmentResult());
       }
@@ -277,7 +278,7 @@ public abstract class TermGroupFacetCollector extends AbstractGroupFacetCollecto
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       if (segmentFacetCounts != null) {
         segmentResults.add(createSegmentResult());
       }
diff --git a/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermSecondPassGroupingCollector.java b/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermSecondPassGroupingCollector.java
index 9401c865a9a..624b0f7c327 100644
-- a/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermSecondPassGroupingCollector.java
++ b/lucene/grouping/src/java/org/apache/lucene/search/grouping/term/TermSecondPassGroupingCollector.java
@@ -22,6 +22,7 @@ import java.util.Collection;
 
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.FieldCache;
 import org.apache.lucene.search.Sort;
 import org.apache.lucene.search.grouping.AbstractSecondPassGroupingCollector;
@@ -53,8 +54,8 @@ public class TermSecondPassGroupingCollector extends AbstractSecondPassGroupingC
   }
 
   @Override
  public void setNextReader(AtomicReaderContext readerContext) throws IOException {
    super.setNextReader(readerContext);
  protected void doSetNextReader(AtomicReaderContext readerContext) throws IOException {
    super.doSetNextReader(readerContext);
     index = FieldCache.DEFAULT.getTermsIndex(readerContext.reader(), groupField);
 
     // Rebuild ordSet
diff --git a/lucene/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterPhraseTest.java b/lucene/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterPhraseTest.java
index 2f7d56dcc57..a84ac7478a5 100644
-- a/lucene/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterPhraseTest.java
++ b/lucene/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterPhraseTest.java
@@ -35,12 +35,13 @@ import org.apache.lucene.index.DirectoryReader;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.Term;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.PhraseQuery;
 import org.apache.lucene.search.Query;
import org.apache.lucene.search.SimpleCollector;
 import org.apache.lucene.search.TopDocs;

 import org.apache.lucene.search.spans.SpanNearQuery;
 import org.apache.lucene.search.spans.SpanQuery;
 import org.apache.lucene.search.spans.SpanTermQuery;
@@ -116,7 +117,7 @@ public class HighlighterPhraseTest extends LuceneTestCase {
           new SpanTermQuery(new Term(FIELD, "fox")),
           new SpanTermQuery(new Term(FIELD, "jumped")) }, 0, true);
       final FixedBitSet bitset = new FixedBitSet(indexReader.maxDoc());
      indexSearcher.search(phraseQuery, new Collector() {
      indexSearcher.search(phraseQuery, new SimpleCollector() {
         private int baseDoc;
 
         @Override
@@ -130,7 +131,7 @@ public class HighlighterPhraseTest extends LuceneTestCase {
         }
 
         @Override
        public void setNextReader(AtomicReaderContext context) {
        protected void doSetNextReader(AtomicReaderContext context) throws IOException {
           this.baseDoc = context.docBase;
         }
 
diff --git a/lucene/join/src/java/org/apache/lucene/search/join/FakeScorer.java b/lucene/join/src/java/org/apache/lucene/search/join/FakeScorer.java
index d4b02dc7d00..cbd1ff8612a 100644
-- a/lucene/join/src/java/org/apache/lucene/search/join/FakeScorer.java
++ b/lucene/join/src/java/org/apache/lucene/search/join/FakeScorer.java
@@ -19,11 +19,11 @@ package org.apache.lucene.search.join;
 
 import java.util.Collection;
 
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.Scorer;
 import org.apache.lucene.search.Weight;
 
/** Passed to {@link Collector#setScorer} during join collection. */
/** Passed to {@link LeafCollector#setScorer} during join collection. */
 final class FakeScorer extends Scorer {
   float score;
   int doc = -1;
diff --git a/lucene/join/src/java/org/apache/lucene/search/join/TermsCollector.java b/lucene/join/src/java/org/apache/lucene/search/join/TermsCollector.java
index 49004b43b35..56545b55502 100644
-- a/lucene/join/src/java/org/apache/lucene/search/join/TermsCollector.java
++ b/lucene/join/src/java/org/apache/lucene/search/join/TermsCollector.java
@@ -22,9 +22,11 @@ import java.io.IOException;
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.BinaryDocValues;
 import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.FieldCache;
 import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.BytesRefHash;
 
@@ -33,7 +35,7 @@ import org.apache.lucene.util.BytesRefHash;
  *
  * @lucene.experimental
  */
abstract class TermsCollector extends Collector {
abstract class TermsCollector extends SimpleCollector {
 
   final String field;
   final BytesRefHash collectorTerms = new BytesRefHash();
@@ -46,10 +48,6 @@ abstract class TermsCollector extends Collector {
     return collectorTerms;
   }
 
  @Override
  public void setScorer(Scorer scorer) throws IOException {
  }

   @Override
   public boolean acceptsDocsOutOfOrder() {
     return true;
@@ -86,7 +84,7 @@ abstract class TermsCollector extends Collector {
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       docTermOrds = FieldCache.DEFAULT.getDocTermOrds(context.reader(), field);
     }
   }
@@ -108,7 +106,7 @@ abstract class TermsCollector extends Collector {
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       fromDocTerms = FieldCache.DEFAULT.getTerms(context.reader(), field, false);
     }
   }
diff --git a/lucene/join/src/java/org/apache/lucene/search/join/TermsIncludingScoreQuery.java b/lucene/join/src/java/org/apache/lucene/search/join/TermsIncludingScoreQuery.java
index 739ef35ed13..220d0e17d88 100644
-- a/lucene/join/src/java/org/apache/lucene/search/join/TermsIncludingScoreQuery.java
++ b/lucene/join/src/java/org/apache/lucene/search/join/TermsIncludingScoreQuery.java
@@ -27,6 +27,7 @@ import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.Terms;
 import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.ComplexExplanation;
 import org.apache.lucene.search.DocIdSetIterator;
@@ -227,7 +228,7 @@ class TermsIncludingScoreQuery extends Query {
     }
 
     @Override
    public boolean score(Collector collector, int max) throws IOException {
    public boolean score(LeafCollector collector, int max) throws IOException {
       FakeScorer fakeScorer = new FakeScorer();
       collector.setScorer(fakeScorer);
       if (doc == -1) {
diff --git a/lucene/join/src/java/org/apache/lucene/search/join/TermsWithScoreCollector.java b/lucene/join/src/java/org/apache/lucene/search/join/TermsWithScoreCollector.java
index dae42b57068..c12f2b9241d 100644
-- a/lucene/join/src/java/org/apache/lucene/search/join/TermsWithScoreCollector.java
++ b/lucene/join/src/java/org/apache/lucene/search/join/TermsWithScoreCollector.java
@@ -22,14 +22,16 @@ import java.io.IOException;
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.BinaryDocValues;
 import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.FieldCache;
 import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
 import org.apache.lucene.util.ArrayUtil;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.BytesRefHash;
 
abstract class TermsWithScoreCollector extends Collector {
abstract class TermsWithScoreCollector extends SimpleCollector {
 
   private final static int INITIAL_ARRAY_SIZE = 256;
 
@@ -128,7 +130,7 @@ abstract class TermsWithScoreCollector extends Collector {
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       fromDocTerms = FieldCache.DEFAULT.getTerms(context.reader(), field, false);
     }
 
@@ -214,7 +216,7 @@ abstract class TermsWithScoreCollector extends Collector {
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       fromDocTermOrds = FieldCache.DEFAULT.getDocTermOrds(context.reader(), field);
     }
 
diff --git a/lucene/join/src/java/org/apache/lucene/search/join/ToParentBlockJoinCollector.java b/lucene/join/src/java/org/apache/lucene/search/join/ToParentBlockJoinCollector.java
index 2e3785d310d..65767fcdbd8 100644
-- a/lucene/join/src/java/org/apache/lucene/search/join/ToParentBlockJoinCollector.java
++ b/lucene/join/src/java/org/apache/lucene/search/join/ToParentBlockJoinCollector.java
@@ -74,7 +74,7 @@ import java.util.*;
  *
  * @lucene.experimental
  */
public class ToParentBlockJoinCollector extends Collector {
public class ToParentBlockJoinCollector extends SimpleCollector {
 
   private final Sort sort;
 
@@ -269,7 +269,7 @@ public class ToParentBlockJoinCollector extends Collector {
   }
 
   @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
  protected void doSetNextReader(AtomicReaderContext context) throws IOException {
     currentReaderContext = context;
     docBase = context.docBase;
     for (int compIDX = 0; compIDX < comparators.length; compIDX++) {
@@ -421,7 +421,7 @@ public class ToParentBlockJoinCollector extends Collector {
       }
 
       collector.setScorer(fakeScorer);
      collector.setNextReader(og.readerContext);
      collector.getLeafCollector(og.readerContext);
       for(int docIDX=0;docIDX<numChildDocs;docIDX++) {
         //System.out.println("docIDX=" + docIDX + " vs " + og.docs[slot].length);
         final int doc = og.docs[slot][docIDX];
diff --git a/lucene/join/src/test/org/apache/lucene/search/join/TestJoinUtil.java b/lucene/join/src/test/org/apache/lucene/search/join/TestJoinUtil.java
index e9f08023c50..f343e51d3fc 100644
-- a/lucene/join/src/test/org/apache/lucene/search/join/TestJoinUtil.java
++ b/lucene/join/src/test/org/apache/lucene/search/join/TestJoinUtil.java
@@ -47,6 +47,7 @@ import org.apache.lucene.index.SortedSetDocValues;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.Terms;
 import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.Collector;
@@ -58,6 +59,7 @@ import org.apache.lucene.search.MatchAllDocsQuery;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.ScoreDoc;
 import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
 import org.apache.lucene.search.TermQuery;
 import org.apache.lucene.search.TopDocs;
 import org.apache.lucene.search.TopScoreDocCollector;
@@ -222,12 +224,9 @@ public class TestJoinUtil extends LuceneTestCase {
     bq.add(joinQuery, BooleanClause.Occur.SHOULD);
     bq.add(new TermQuery(new Term("id", "3")), BooleanClause.Occur.SHOULD);
 
    indexSearcher.search(bq, new Collector() {
    indexSearcher.search(bq, new SimpleCollector() {
         boolean sawFive;
         @Override
        public void setNextReader(AtomicReaderContext context) {
        }
        @Override
         public void collect(int docID) {
           // Hairy / evil (depends on how BooleanScorer
           // stores temporarily collected docIDs by
@@ -239,9 +238,6 @@ public class TestJoinUtil extends LuceneTestCase {
           }
         }
         @Override
        public void setScorer(Scorer scorer) {
        }
        @Override
         public boolean acceptsDocsOutOfOrder() {
           return true;
         }
@@ -407,7 +403,7 @@ public class TestJoinUtil extends LuceneTestCase {
         // Need to know all documents that have matches. TopDocs doesn't give me that and then I'd be also testing TopDocsCollector...
         final FixedBitSet actualResult = new FixedBitSet(indexSearcher.getIndexReader().maxDoc());
         final TopScoreDocCollector topScoreDocCollector = TopScoreDocCollector.create(10, false);
        indexSearcher.search(joinQuery, new Collector() {
        indexSearcher.search(joinQuery, new SimpleCollector() {
 
           int docBase;
 
@@ -418,9 +414,9 @@ public class TestJoinUtil extends LuceneTestCase {
           }
 
           @Override
          public void setNextReader(AtomicReaderContext context) {
          protected void doSetNextReader(AtomicReaderContext context) throws IOException {
             docBase = context.docBase;
            topScoreDocCollector.setNextReader(context);
            topScoreDocCollector.getLeafCollector(context);
           }
 
           @Override
@@ -572,7 +568,7 @@ public class TestJoinUtil extends LuceneTestCase {
       }
       final Map<BytesRef, JoinScore> joinValueToJoinScores = new HashMap<>();
       if (multipleValuesPerDocument) {
        fromSearcher.search(new TermQuery(new Term("value", uniqueRandomValue)), new Collector() {
        fromSearcher.search(new TermQuery(new Term("value", uniqueRandomValue)), new SimpleCollector() {
 
           private Scorer scorer;
           private SortedSetDocValues docTermOrds;
@@ -593,7 +589,7 @@ public class TestJoinUtil extends LuceneTestCase {
           }
 
           @Override
          public void setNextReader(AtomicReaderContext context) throws IOException {
          protected void doSetNextReader(AtomicReaderContext context) throws IOException {
             docTermOrds = FieldCache.DEFAULT.getDocTermOrds(context.reader(), fromField);
           }
 
@@ -608,7 +604,7 @@ public class TestJoinUtil extends LuceneTestCase {
           }
         });
       } else {
        fromSearcher.search(new TermQuery(new Term("value", uniqueRandomValue)), new Collector() {
        fromSearcher.search(new TermQuery(new Term("value", uniqueRandomValue)), new SimpleCollector() {
 
           private Scorer scorer;
           private BinaryDocValues terms;
@@ -631,7 +627,7 @@ public class TestJoinUtil extends LuceneTestCase {
           }
 
           @Override
          public void setNextReader(AtomicReaderContext context) throws IOException {
          protected void doSetNextReader(AtomicReaderContext context) throws IOException {
             terms = FieldCache.DEFAULT.getTerms(context.reader(), fromField, true);
             docsWithField = FieldCache.DEFAULT.getDocsWithField(context.reader(), fromField);
           }
@@ -675,7 +671,7 @@ public class TestJoinUtil extends LuceneTestCase {
             }
           }
         } else {
          toSearcher.search(new MatchAllDocsQuery(), new Collector() {
          toSearcher.search(new MatchAllDocsQuery(), new SimpleCollector() {
 
             private SortedSetDocValues docTermOrds;
             private final BytesRef scratch = new BytesRef();
@@ -701,7 +697,7 @@ public class TestJoinUtil extends LuceneTestCase {
             }
 
             @Override
            public void setNextReader(AtomicReaderContext context) throws IOException {
            protected void doSetNextReader(AtomicReaderContext context) throws IOException {
               docBase = context.docBase;
               docTermOrds = FieldCache.DEFAULT.getDocTermOrds(context.reader(), toField);
             }
@@ -713,7 +709,7 @@ public class TestJoinUtil extends LuceneTestCase {
           });
         }
       } else {
        toSearcher.search(new MatchAllDocsQuery(), new Collector() {
        toSearcher.search(new MatchAllDocsQuery(), new SimpleCollector() {
 
           private BinaryDocValues terms;
           private int docBase;
@@ -730,7 +726,7 @@ public class TestJoinUtil extends LuceneTestCase {
           }
 
           @Override
          public void setNextReader(AtomicReaderContext context) throws IOException {
          protected void doSetNextReader(AtomicReaderContext context) throws IOException {
             terms = FieldCache.DEFAULT.getTerms(context.reader(), toField, false);
             docBase = context.docBase;
           }
diff --git a/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java b/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
index db79ff021fd..a76877445b3 100644
-- a/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
++ b/lucene/memory/src/java/org/apache/lucene/index/memory/MemoryIndex.java
@@ -54,6 +54,7 @@ import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
 import org.apache.lucene.search.similarities.Similarity;
 import org.apache.lucene.store.RAMDirectory; // for javadocs
 import org.apache.lucene.util.ArrayUtil;
@@ -532,7 +533,7 @@ public class MemoryIndex {
     IndexSearcher searcher = createSearcher();
     try {
       final float[] scores = new float[1]; // inits to 0.0f (no match)
      searcher.search(query, new Collector() {
      searcher.search(query, new SimpleCollector() {
         private Scorer scorer;
 
         @Override
@@ -550,8 +551,6 @@ public class MemoryIndex {
           return true;
         }
 
        @Override
        public void setNextReader(AtomicReaderContext context) { }
       });
       float score = scores[0];
       return score;
diff --git a/lucene/misc/src/java/org/apache/lucene/index/sorter/EarlyTerminatingSortingCollector.java b/lucene/misc/src/java/org/apache/lucene/index/sorter/EarlyTerminatingSortingCollector.java
index 23772e18f23..2571632defd 100644
-- a/lucene/misc/src/java/org/apache/lucene/index/sorter/EarlyTerminatingSortingCollector.java
++ b/lucene/misc/src/java/org/apache/lucene/index/sorter/EarlyTerminatingSortingCollector.java
@@ -21,9 +21,11 @@ import java.io.IOException;
 
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.CollectionTerminatedException;
 import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.FilterLeafCollector;
import org.apache.lucene.search.FilterCollector;
 import org.apache.lucene.search.Sort;
 import org.apache.lucene.search.TopDocsCollector;
 import org.apache.lucene.search.TotalHitCountCollector;
@@ -32,11 +34,11 @@ import org.apache.lucene.search.TotalHitCountCollector;
  * A {@link Collector} that early terminates collection of documents on a
  * per-segment basis, if the segment was sorted according to the given
  * {@link Sort}.
 * 
 *
  * <p>
  * <b>NOTE:</b> the {@code Collector} detects sorted segments according to
  * {@link SortingMergePolicy}, so it's best used in conjunction with it. Also,
 * it collects up to a specified {@code numDocsToCollect} from each segment, 
 * it collects up to a specified {@code numDocsToCollect} from each segment,
  * and therefore is mostly suitable for use in conjunction with collectors such as
  * {@link TopDocsCollector}, and not e.g. {@link TotalHitCountCollector}.
  * <p>
@@ -58,26 +60,21 @@ import org.apache.lucene.search.TotalHitCountCollector;
  * the old and the new {@code Sort}s have the same identifier, this
  * {@code Collector} will incorrectly detect sorted segments.</li>
  * </ul>
 * 
 *
  * @lucene.experimental
  */
public class EarlyTerminatingSortingCollector extends Collector {
  /** The wrapped Collector */
  protected final Collector in;
public class EarlyTerminatingSortingCollector extends FilterCollector {

   /** Sort used to sort the search results */
   protected final Sort sort;
   /** Number of documents to collect in each segment */
   protected final int numDocsToCollect;
  /** Number of documents to collect in the current segment being processed */
  protected int segmentTotalCollect;
  /** True if the current segment being processed is sorted by {@link #sort} */
  protected boolean segmentSorted;
 
   private int numCollected;
 
   /**
    * Create a new {@link EarlyTerminatingSortingCollector} instance.
   * 
   *
    * @param in
    *          the collector to wrap
    * @param sort
@@ -88,38 +85,37 @@ public class EarlyTerminatingSortingCollector extends Collector {
    *          hits.
    */
   public EarlyTerminatingSortingCollector(Collector in, Sort sort, int numDocsToCollect) {
    super(in);
     if (numDocsToCollect <= 0) {
      throw new IllegalStateException("numDocsToCollect must always be > 0, got " + segmentTotalCollect);
      throw new IllegalStateException("numDocsToCollect must always be > 0, got " + numDocsToCollect);
     }
    this.in = in;
     this.sort = sort;
     this.numDocsToCollect = numDocsToCollect;
   }
 
   @Override
  public void setScorer(Scorer scorer) throws IOException {
    in.setScorer(scorer);
  }
  public LeafCollector getLeafCollector(AtomicReaderContext context) throws IOException {
    if (SortingMergePolicy.isSorted(context.reader(), sort)) {
      // segment is sorted, can early-terminate
      return new FilterLeafCollector(super.getLeafCollector(context)) {
 
  @Override
  public void collect(int doc) throws IOException {
    in.collect(doc);
    if (++numCollected >= segmentTotalCollect) {
      throw new CollectionTerminatedException();
    }
  }
        @Override
        public void collect(int doc) throws IOException {
          super.collect(doc);
          if (++numCollected >= numDocsToCollect) {
            throw new CollectionTerminatedException();
          }
        }
 
  @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
    in.setNextReader(context);
    segmentSorted = SortingMergePolicy.isSorted(context.reader(), sort);
    segmentTotalCollect = segmentSorted ? numDocsToCollect : Integer.MAX_VALUE;
    numCollected = 0;
  }
        @Override
        public boolean acceptsDocsOutOfOrder() {
          return false;
        }
 
  @Override
  public boolean acceptsDocsOutOfOrder() {
    return !segmentSorted && in.acceptsDocsOutOfOrder();
      };
    } else {
      return super.getLeafCollector(context);
    }
   }
 
 }
diff --git a/lucene/misc/src/test/org/apache/lucene/index/sorter/TestEarlyTermination.java b/lucene/misc/src/test/org/apache/lucene/index/sorter/TestEarlyTermination.java
index f64f56de711..716cc200be6 100644
-- a/lucene/misc/src/test/org/apache/lucene/index/sorter/TestEarlyTermination.java
++ b/lucene/misc/src/test/org/apache/lucene/index/sorter/TestEarlyTermination.java
@@ -34,6 +34,7 @@ import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.ScoreDoc;
 import org.apache.lucene.search.Sort;
@@ -147,9 +148,10 @@ public class TestEarlyTermination extends LuceneTestCase {
       Sort different = new Sort(new SortField("ndv2", SortField.Type.LONG));
       searcher.search(query, new EarlyTerminatingSortingCollector(collector2, different, numHits) {
         @Override
        public void setNextReader(AtomicReaderContext context) throws IOException {
          super.setNextReader(context);
          assertFalse("segment should not be recognized as sorted as different sorter was used", segmentSorted);
        public LeafCollector getLeafCollector(AtomicReaderContext context) throws IOException {
          final LeafCollector ret = super.getLeafCollector(context);
          assertTrue("segment should not be recognized as sorted as different sorter was used", ret.getClass() == in.getLeafCollector(context).getClass());
          return ret;
         }
       });
     }
diff --git a/lucene/queryparser/src/test/org/apache/lucene/queryparser/surround/query/BooleanQueryTst.java b/lucene/queryparser/src/test/org/apache/lucene/queryparser/surround/query/BooleanQueryTst.java
index 35a402deb14..4c6349b32d5 100644
-- a/lucene/queryparser/src/test/org/apache/lucene/queryparser/surround/query/BooleanQueryTst.java
++ b/lucene/queryparser/src/test/org/apache/lucene/queryparser/surround/query/BooleanQueryTst.java
@@ -22,13 +22,13 @@ import java.io.IOException;
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.DirectoryReader;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.Scorer;
 import org.apache.lucene.search.Query;

import org.apache.lucene.search.SimpleCollector;
 import org.apache.lucene.queryparser.surround.parser.QueryParser;

 import org.junit.Assert;
 
 public class BooleanQueryTst {
@@ -57,7 +57,7 @@ public class BooleanQueryTst {
   
   public void setVerbose(boolean verbose) {this.verbose = verbose;}
 
  class TestCollector extends Collector { // FIXME: use check hits from Lucene tests
  class TestCollector extends SimpleCollector { // FIXME: use check hits from Lucene tests
     int totalMatched;
     boolean[] encountered;
     private Scorer scorer = null;
@@ -79,7 +79,7 @@ public class BooleanQueryTst {
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       docBase = context.docBase;
     }
     
diff --git a/lucene/test-framework/src/java/org/apache/lucene/search/AssertingBulkOutOfOrderScorer.java b/lucene/test-framework/src/java/org/apache/lucene/search/AssertingBulkOutOfOrderScorer.java
index 0b2fa34b044..39aa3c68b50 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/search/AssertingBulkOutOfOrderScorer.java
++ b/lucene/test-framework/src/java/org/apache/lucene/search/AssertingBulkOutOfOrderScorer.java
@@ -59,7 +59,7 @@ public class AssertingBulkOutOfOrderScorer extends BulkScorer {
   }
 
   private static void flush(int[] docIDs, float[] scores, int[] freqs, int size,
      FakeScorer scorer, Collector collector) throws IOException {
      FakeScorer scorer, LeafCollector collector) throws IOException {
     for (int i = 0; i < size; ++i) {
       scorer.doc = docIDs[i];
       scorer.freq = freqs[i];
@@ -69,7 +69,7 @@ public class AssertingBulkOutOfOrderScorer extends BulkScorer {
   }
 
   @Override
  public boolean score(Collector collector, int max) throws IOException {
  public boolean score(LeafCollector collector, int max) throws IOException {
     if (scorer.docID() == -1) {
       scorer.nextDoc();
     }
diff --git a/lucene/test-framework/src/java/org/apache/lucene/search/AssertingBulkScorer.java b/lucene/test-framework/src/java/org/apache/lucene/search/AssertingBulkScorer.java
index 995f49aee1d..50114aa6485 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/search/AssertingBulkScorer.java
++ b/lucene/test-framework/src/java/org/apache/lucene/search/AssertingBulkScorer.java
@@ -31,8 +31,8 @@ import org.apache.lucene.util.VirtualMethod;
 /** Wraps a Scorer with additional checks */
 public class AssertingBulkScorer extends BulkScorer {
 
  private static final VirtualMethod<BulkScorer> SCORE_COLLECTOR = new VirtualMethod<BulkScorer>(BulkScorer.class, "score", Collector.class);
  private static final VirtualMethod<BulkScorer> SCORE_COLLECTOR_RANGE = new VirtualMethod<BulkScorer>(BulkScorer.class, "score", Collector.class, int.class);
  private static final VirtualMethod<BulkScorer> SCORE_COLLECTOR = new VirtualMethod<BulkScorer>(BulkScorer.class, "score", LeafCollector.class);
  private static final VirtualMethod<BulkScorer> SCORE_COLLECTOR_RANGE = new VirtualMethod<BulkScorer>(BulkScorer.class, "score", LeafCollector.class, int.class);
 
   public static BulkScorer wrap(Random random, BulkScorer other) {
     if (other == null || other instanceof AssertingBulkScorer) {
@@ -58,7 +58,7 @@ public class AssertingBulkScorer extends BulkScorer {
   }
 
   @Override
  public void score(Collector collector) throws IOException {
  public void score(LeafCollector collector) throws IOException {
     if (random.nextBoolean()) {
       try {
         final boolean remaining = in.score(collector, DocsEnum.NO_MORE_DOCS);
@@ -72,7 +72,7 @@ public class AssertingBulkScorer extends BulkScorer {
   }
 
   @Override
  public boolean score(Collector collector, int max) throws IOException {
  public boolean score(LeafCollector collector, int max) throws IOException {
     return in.score(collector, max);
   }
 
diff --git a/lucene/test-framework/src/java/org/apache/lucene/search/AssertingCollector.java b/lucene/test-framework/src/java/org/apache/lucene/search/AssertingCollector.java
index 8ab292614fc..7aa8a2ef3f5 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/search/AssertingCollector.java
++ b/lucene/test-framework/src/java/org/apache/lucene/search/AssertingCollector.java
@@ -25,46 +25,42 @@ import org.apache.lucene.index.AtomicReaderContext;
 /** Wraps another Collector and checks that
  *  acceptsDocsOutOfOrder is respected. */
 
public class AssertingCollector extends Collector {
public class AssertingCollector extends FilterCollector {
 
   public static Collector wrap(Random random, Collector other, boolean inOrder) {
     return other instanceof AssertingCollector ? other : new AssertingCollector(random, other, inOrder);
   }
 
   final Random random;
  final Collector in;
   final boolean inOrder;
  int lastCollected;
 
   AssertingCollector(Random random, Collector in, boolean inOrder) {
    super(in);
     this.random = random;
    this.in = in;
     this.inOrder = inOrder;
    lastCollected = -1;
   }
 
   @Override
  public void setScorer(Scorer scorer) throws IOException {
    in.setScorer(AssertingScorer.getAssertingScorer(random, scorer));
  }
  public LeafCollector getLeafCollector(AtomicReaderContext context) throws IOException {
    return new FilterLeafCollector(super.getLeafCollector(context)) {
 
  @Override
  public void collect(int doc) throws IOException {
    if (inOrder || !acceptsDocsOutOfOrder()) {
      assert doc > lastCollected : "Out of order : " + lastCollected + " " + doc;
    }
    in.collect(doc);
    lastCollected = doc;
  }
      int lastCollected = -1;
 
  @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
    lastCollected = -1;
  }
      @Override
      public void setScorer(Scorer scorer) throws IOException {
        super.setScorer(AssertingScorer.getAssertingScorer(random, scorer));
      }
 
  @Override
  public boolean acceptsDocsOutOfOrder() {
    return in.acceptsDocsOutOfOrder();
      @Override
      public void collect(int doc) throws IOException {
        if (inOrder || !acceptsDocsOutOfOrder()) {
          assert doc > lastCollected : "Out of order : " + lastCollected + " " + doc;
        }
        in.collect(doc);
        lastCollected = doc;
      }

    };
   }
 
 }
diff --git a/lucene/test-framework/src/java/org/apache/lucene/search/CheckHits.java b/lucene/test-framework/src/java/org/apache/lucene/search/CheckHits.java
index 034396c9a05..042ad9b1eb3 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/search/CheckHits.java
++ b/lucene/test-framework/src/java/org/apache/lucene/search/CheckHits.java
@@ -123,7 +123,7 @@ public class CheckHits {
   /**
    * Just collects document ids into a set.
    */
  public static class SetCollector extends Collector {
  public static class SetCollector extends SimpleCollector {
     final Set<Integer> bag;
     public SetCollector(Set<Integer> bag) {
       this.bag = bag;
@@ -136,7 +136,7 @@ public class CheckHits {
       bag.add(Integer.valueOf(doc + base));
     }
     @Override
    public void setNextReader(AtomicReaderContext context) {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       base = context.docBase;
     }
     @Override
@@ -464,7 +464,7 @@ public class CheckHits {
    *
    * @see CheckHits#verifyExplanation
    */
  public static class ExplanationAsserter extends Collector {
  public static class ExplanationAsserter extends SimpleCollector {
 
     Query q;
     IndexSearcher s;
@@ -508,7 +508,7 @@ public class CheckHits {
                         exp.isMatch());
     }
     @Override
    public void setNextReader(AtomicReaderContext context) {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       base = context.docBase;
     }
     @Override
diff --git a/lucene/test-framework/src/java/org/apache/lucene/search/QueryUtils.java b/lucene/test-framework/src/java/org/apache/lucene/search/QueryUtils.java
index cd00f74a12d..8656ef2eab8 100644
-- a/lucene/test-framework/src/java/org/apache/lucene/search/QueryUtils.java
++ b/lucene/test-framework/src/java/org/apache/lucene/search/QueryUtils.java
@@ -249,7 +249,7 @@ public class QueryUtils {
         final float maxDiff = 1e-5f;
         final AtomicReader lastReader[] = {null};
 
        s.search(q, new Collector() {
        s.search(q, new SimpleCollector() {
           private Scorer sc;
           private Scorer scorer;
           private int leafPtr;
@@ -305,7 +305,7 @@ public class QueryUtils {
           }
 
           @Override
          public void setNextReader(AtomicReaderContext context) throws IOException {
          protected void doSetNextReader(AtomicReaderContext context) throws IOException {
             // confirm that skipping beyond the last doc, on the
             // previous reader, hits NO_MORE_DOCS
             if (lastReader[0] != null) {
@@ -357,7 +357,7 @@ public class QueryUtils {
     final int lastDoc[] = {-1};
     final AtomicReader lastReader[] = {null};
     final List<AtomicReaderContext> context = s.getTopReaderContext().leaves();
    s.search(q,new Collector() {
    s.search(q,new SimpleCollector() {
       private Scorer scorer;
       private int leafPtr;
       private Bits liveDocs;
@@ -392,7 +392,7 @@ public class QueryUtils {
       }
 
       @Override
      public void setNextReader(AtomicReaderContext context) throws IOException {
      protected void doSetNextReader(AtomicReaderContext context) throws IOException {
         // confirm that skipping beyond the last doc, on the
         // previous reader, hits NO_MORE_DOCS
         if (lastReader[0] != null) {
diff --git a/solr/core/src/java/org/apache/solr/analytics/accumulator/BasicAccumulator.java b/solr/core/src/java/org/apache/solr/analytics/accumulator/BasicAccumulator.java
index 304c0a2b5a0..fdcf66ba2d5 100644
-- a/solr/core/src/java/org/apache/solr/analytics/accumulator/BasicAccumulator.java
++ b/solr/core/src/java/org/apache/solr/analytics/accumulator/BasicAccumulator.java
@@ -80,7 +80,7 @@ public class BasicAccumulator extends ValueAccumulator {
   }
   
   @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
  protected void doSetNextReader(AtomicReaderContext context) throws IOException {
     this.context = context;
     for (StatsCollector counter : statsCollectors) {
       counter.setNextReader(context);
diff --git a/solr/core/src/java/org/apache/solr/analytics/accumulator/FacetingAccumulator.java b/solr/core/src/java/org/apache/solr/analytics/accumulator/FacetingAccumulator.java
index 61ed6e100b9..fb6d81d5bf9 100644
-- a/solr/core/src/java/org/apache/solr/analytics/accumulator/FacetingAccumulator.java
++ b/solr/core/src/java/org/apache/solr/analytics/accumulator/FacetingAccumulator.java
@@ -155,8 +155,8 @@ public class FacetingAccumulator extends BasicAccumulator implements FacetValueA
    * @throws IOException if there is an error setting the next reader
    */
   @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
    super.setNextReader(context);
  protected void doSetNextReader(AtomicReaderContext context) throws IOException {
    super.doSetNextReader(context);
     for( Map<String,StatsCollector[]> valueList : fieldFacetCollectors.values() ){
       for (StatsCollector[] statsCollectorList : valueList.values()) {
         for (StatsCollector statsCollector : statsCollectorList) {
@@ -165,7 +165,7 @@ public class FacetingAccumulator extends BasicAccumulator implements FacetValueA
       }
     }
     for (FieldFacetAccumulator fa : facetAccumulators) {
      fa.setNextReader(context);
      fa.getLeafCollector(context);
     }
   }
   
@@ -175,7 +175,7 @@ public class FacetingAccumulator extends BasicAccumulator implements FacetValueA
    * @throws IOException if there is an error setting the next reader
    */
   public void setRangeStatsCollectorReaders(AtomicReaderContext context) throws IOException {
    super.setNextReader(context);
    super.getLeafCollector(context);
     for( Map<String,StatsCollector[]> rangeList : rangeFacetCollectors.values() ){
       for (StatsCollector[] statsCollectorList : rangeList.values()) {
         for (StatsCollector statsCollector : statsCollectorList) {
@@ -192,7 +192,7 @@ public class FacetingAccumulator extends BasicAccumulator implements FacetValueA
    * @throws IOException if there is an error setting the next reader
    */
   public void setQueryStatsCollectorReaders(AtomicReaderContext context) throws IOException {
    super.setNextReader(context);
    super.getLeafCollector(context);
     for( Map<String,StatsCollector[]> queryList : queryFacetCollectors.values() ){
       for (StatsCollector[] statsCollectorList : queryList.values()) {
         for (StatsCollector statsCollector : statsCollectorList) {
diff --git a/solr/core/src/java/org/apache/solr/analytics/accumulator/ValueAccumulator.java b/solr/core/src/java/org/apache/solr/analytics/accumulator/ValueAccumulator.java
index ecc74ef01d5..90b8713f2bd 100644
-- a/solr/core/src/java/org/apache/solr/analytics/accumulator/ValueAccumulator.java
++ b/solr/core/src/java/org/apache/solr/analytics/accumulator/ValueAccumulator.java
@@ -20,20 +20,14 @@ package org.apache.solr.analytics.accumulator;
 import java.io.IOException;
 
 import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.SimpleCollector;
 import org.apache.solr.common.util.NamedList;
 
 /**
  * Abstract Collector that manages all StatsCollectors, Expressions and Facets.
  */
public abstract class ValueAccumulator extends Collector {

  /**
   * @param context The context to read documents from.
   * @throws IOException if setting next reader fails
   */
  public abstract void setNextReader(AtomicReaderContext context) throws IOException;
public abstract class ValueAccumulator extends SimpleCollector {
   
   /**
    * Finalizes the statistics within each StatsCollector.
@@ -51,9 +45,4 @@ public abstract class ValueAccumulator extends Collector {
     return true;
   }
 
  @Override
  public void setScorer(Scorer scorer) throws IOException {
    // NOP
  }
  
 }
diff --git a/solr/core/src/java/org/apache/solr/analytics/accumulator/facet/FieldFacetAccumulator.java b/solr/core/src/java/org/apache/solr/analytics/accumulator/facet/FieldFacetAccumulator.java
index 937690917ed..e2cf4168452 100644
-- a/solr/core/src/java/org/apache/solr/analytics/accumulator/facet/FieldFacetAccumulator.java
++ b/solr/core/src/java/org/apache/solr/analytics/accumulator/facet/FieldFacetAccumulator.java
@@ -82,7 +82,7 @@ public class FieldFacetAccumulator extends ValueAccumulator {
    * Move to the next set of documents to add to the field facet.
    */
   @Override
  public void setNextReader(AtomicReaderContext context) throws IOException { 
  protected void doSetNextReader(AtomicReaderContext context) throws IOException {
     if (multiValued) {
       setValues = context.reader().getSortedSetDocValues(name);
     } else {
diff --git a/solr/core/src/java/org/apache/solr/analytics/accumulator/facet/QueryFacetAccumulator.java b/solr/core/src/java/org/apache/solr/analytics/accumulator/facet/QueryFacetAccumulator.java
index f0d6b4aa516..3a268eebffa 100644
-- a/solr/core/src/java/org/apache/solr/analytics/accumulator/facet/QueryFacetAccumulator.java
++ b/solr/core/src/java/org/apache/solr/analytics/accumulator/facet/QueryFacetAccumulator.java
@@ -51,7 +51,7 @@ public class QueryFacetAccumulator extends ValueAccumulator {
    * Update the readers of the queryFacet {@link StatsCollector}s in FacetingAccumulator
    */
   @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
  protected void doSetNextReader(AtomicReaderContext context) throws IOException {
     parent.setQueryStatsCollectorReaders(context);
   }
 
diff --git a/solr/core/src/java/org/apache/solr/analytics/accumulator/facet/RangeFacetAccumulator.java b/solr/core/src/java/org/apache/solr/analytics/accumulator/facet/RangeFacetAccumulator.java
index dd29c1c414b..8c07c4f134d 100644
-- a/solr/core/src/java/org/apache/solr/analytics/accumulator/facet/RangeFacetAccumulator.java
++ b/solr/core/src/java/org/apache/solr/analytics/accumulator/facet/RangeFacetAccumulator.java
@@ -43,7 +43,7 @@ public class RangeFacetAccumulator extends QueryFacetAccumulator {
    * Update the readers of the rangeFacet {@link StatsCollector}s in FacetingAccumulator
    */
   @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
  protected void doSetNextReader(AtomicReaderContext context) throws IOException {
     parent.setRangeStatsCollectorReaders(context);
   }
 
diff --git a/solr/core/src/java/org/apache/solr/analytics/request/AnalyticsStats.java b/solr/core/src/java/org/apache/solr/analytics/request/AnalyticsStats.java
index c1ec21fb15b..adc68074023 100644
-- a/solr/core/src/java/org/apache/solr/analytics/request/AnalyticsStats.java
++ b/solr/core/src/java/org/apache/solr/analytics/request/AnalyticsStats.java
@@ -113,7 +113,7 @@ public class AnalyticsStats {
         }
 
         if (disi != null) {
          accumulator.setNextReader(context);
          accumulator.getLeafCollector(context);
           int doc = disi.nextDoc();
           while( doc != DocIdSetIterator.NO_MORE_DOCS){
             // Add a document to the statistics being generated
diff --git a/solr/core/src/java/org/apache/solr/handler/component/ExpandComponent.java b/solr/core/src/java/org/apache/solr/handler/component/ExpandComponent.java
index 99465b7f347..64b5690b69e 100644
-- a/solr/core/src/java/org/apache/solr/handler/component/ExpandComponent.java
++ b/solr/core/src/java/org/apache/solr/handler/component/ExpandComponent.java
@@ -20,9 +20,11 @@ package org.apache.solr.handler.component;
 import org.apache.lucene.index.AtomicReader;
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.DocIdSetIterator;
 import org.apache.lucene.search.ScoreDoc;
 import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
 import org.apache.lucene.search.Sort;
 import org.apache.lucene.search.TopDocs;
 import org.apache.lucene.search.FieldCache;
@@ -52,9 +54,12 @@ import org.apache.solr.util.plugin.PluginInfoInitialized;
 import org.apache.solr.util.plugin.SolrCoreAware;
 import org.apache.solr.core.PluginInfo;
 import org.apache.solr.core.SolrCore;
import com.carrotsearch.hppc.IntObjectMap;
 import com.carrotsearch.hppc.IntObjectOpenHashMap;
 import com.carrotsearch.hppc.IntOpenHashSet;
 import com.carrotsearch.hppc.cursors.IntObjectCursor;
import com.carrotsearch.hppc.cursors.ObjectCursor;

 import java.io.IOException;
 import java.net.MalformedURLException;
 import java.net.URL;
@@ -210,9 +215,9 @@ public class ExpandComponent extends SearchComponent implements PluginInfoInitia
     }
 
     searcher.search(query, pfilter.filter, collector);
    IntObjectOpenHashMap groups = groupExpandCollector.getGroups();
    IntObjectMap groups = groupExpandCollector.getGroups();
     Iterator<IntObjectCursor> it = groups.iterator();
    Map<String, DocSlice> outMap = new HashMap();
    Map<String, DocSlice> outMap = new HashMap<>();
     BytesRef bytesRef = new BytesRef();
     CharsRef charsRef = new CharsRef();
     FieldType fieldType = searcher.getSchema().getField(field).getType();
@@ -292,24 +297,21 @@ public class ExpandComponent extends SearchComponent implements PluginInfoInitia
     rb.rsp.add("expanded", expanded);
   }
 
  private class GroupExpandCollector extends Collector {
  private class GroupExpandCollector implements Collector {
     private SortedDocValues docValues;
    private IntObjectOpenHashMap groups;
    private IntObjectMap<Collector> groups;
     private int docBase;
     private FixedBitSet groupBits;
     private IntOpenHashSet collapsedSet;
    private List<Collector> collectors;
 
     public GroupExpandCollector(SortedDocValues docValues, FixedBitSet groupBits, IntOpenHashSet collapsedSet, int limit, Sort sort) throws IOException {
       int numGroups = collapsedSet.size();
      groups = new IntObjectOpenHashMap(numGroups*2);
      collectors = new ArrayList();
      groups = new IntObjectOpenHashMap<>(numGroups*2);
       DocIdSetIterator iterator = groupBits.iterator();
       int group = -1;
       while((group = iterator.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
         Collector collector = (sort == null) ? TopScoreDocCollector.create(limit, true) : TopFieldCollector.create(sort,limit, false, false,false, true);
         groups.put(group, collector);
        collectors.add(collector);
       }
 
       this.collapsedSet = collapsedSet;
@@ -317,35 +319,42 @@ public class ExpandComponent extends SearchComponent implements PluginInfoInitia
       this.docValues = docValues;
     }
 
    public IntObjectOpenHashMap getGroups() {
      return this.groups;
    }

    public boolean acceptsDocsOutOfOrder() {
      return false;
    }

    public void collect(int docId) throws IOException {
      int doc = docId+docBase;
      int ord = docValues.getOrd(doc);
      if(ord > -1 && groupBits.get(ord) && !collapsedSet.contains(doc)) {
        Collector c = (Collector)groups.get(ord);
        c.collect(docId);
    public LeafCollector getLeafCollector(AtomicReaderContext context) throws IOException {
      final int docBase = context.docBase;
      final IntObjectMap<LeafCollector> leafCollectors = new IntObjectOpenHashMap<>();
      for (IntObjectCursor<Collector> entry : groups) {
        leafCollectors.put(entry.key, entry.value.getLeafCollector(context));
       }
      return new LeafCollector() {
        
        @Override
        public void setScorer(Scorer scorer) throws IOException {
          for (ObjectCursor<LeafCollector> c : leafCollectors.values()) {
            c.value.setScorer(scorer);
          }
        }
        
        @Override
        public void collect(int docId) throws IOException {
          int doc = docId+docBase;
          int ord = docValues.getOrd(doc);
          if(ord > -1 && groupBits.get(ord) && !collapsedSet.contains(doc)) {
            LeafCollector c = leafCollectors.get(ord);
            c.collect(docId);
          }
        }
        
        @Override
        public boolean acceptsDocsOutOfOrder() {
          return false;
        }
      };
     }
 
    public void setNextReader(AtomicReaderContext context) throws IOException {
      this.docBase = context.docBase;
      for(Collector c : collectors) {
        c.setNextReader(context);
      }
    public IntObjectMap<Collector> getGroups() {
      return groups;
     }
 
    public void setScorer(Scorer scorer) throws IOException {
      for(Collector c : collectors) {
        c.setScorer(scorer);
      }
    }
   }
 
   ////////////////////////////////////////////
@@ -372,4 +381,4 @@ public class ExpandComponent extends SearchComponent implements PluginInfoInitia
       throw new RuntimeException(e);
     }
   }
}
\ No newline at end of file
}
diff --git a/solr/core/src/java/org/apache/solr/schema/LatLonType.java b/solr/core/src/java/org/apache/solr/schema/LatLonType.java
index 27157d27ce0..2763c8439cb 100644
-- a/solr/core/src/java/org/apache/solr/schema/LatLonType.java
++ b/solr/core/src/java/org/apache/solr/schema/LatLonType.java
@@ -23,6 +23,7 @@ import java.util.Map;
 import java.util.Set;
 
 import com.spatial4j.core.shape.Point;

 import org.apache.lucene.document.FieldType;
 import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.IndexReader;
@@ -30,6 +31,7 @@ import org.apache.lucene.index.StorableField;
 import org.apache.lucene.queries.function.FunctionValues;
 import org.apache.lucene.queries.function.ValueSource;
 import org.apache.lucene.queries.function.valuesource.VectorValueSource;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.ComplexExplanation;
@@ -51,6 +53,7 @@ import org.apache.solr.search.SpatialOptions;
 import com.spatial4j.core.context.SpatialContext;
 import com.spatial4j.core.distance.DistanceUtils;
 import com.spatial4j.core.shape.Rectangle;

 import org.apache.solr.util.SpatialUtils;
 
 
@@ -522,14 +525,14 @@ class SpatialDistanceQuery extends ExtendedQueryBase implements PostFilter {
     @Override
     public void collect(int doc) throws IOException {
       spatialScorer.doc = doc;
      if (spatialScorer.match()) delegate.collect(doc);
      if (spatialScorer.match()) leafDelegate.collect(doc);
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
      super.doSetNextReader(context);
       maxdoc = context.reader().maxDoc();
       spatialScorer = new SpatialScorer(context, null, weight, 1.0f);
      super.setNextReader(context);
     }
   }
 
diff --git a/solr/core/src/java/org/apache/solr/search/CollapsingQParserPlugin.java b/solr/core/src/java/org/apache/solr/search/CollapsingQParserPlugin.java
index 601790c41f5..93ce79cd928 100644
-- a/solr/core/src/java/org/apache/solr/search/CollapsingQParserPlugin.java
++ b/solr/core/src/java/org/apache/solr/search/CollapsingQParserPlugin.java
@@ -34,11 +34,15 @@ import org.apache.lucene.index.TermsEnum;
 import org.apache.lucene.queries.function.FunctionQuery;
 import org.apache.lucene.queries.function.FunctionValues;
 import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.DocIdSetIterator;
 import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.FilterCollector;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopFieldCollector;
 import org.apache.lucene.util.Bits;
 import org.apache.lucene.util.BytesRef;
 import org.apache.lucene.util.FixedBitSet;
@@ -340,7 +344,7 @@ public class CollapsingQParserPlugin extends QParserPlugin {
 
         IntOpenHashSet boostDocs = getBoostDocs(searcher, this.boosted);
 
        if(this.min != null || this.max != null) {
        if (this.min != null || this.max != null) {
 
           return new CollapsingFieldValueCollector(maxDoc,
                                                    leafCount,
@@ -436,7 +440,6 @@ public class CollapsingQParserPlugin extends QParserPlugin {
     private SortedDocValues values;
     private int[] ords;
     private float[] scores;
    private int docBase;
     private int maxDoc;
     private int nullPolicy;
     private float nullScore = -Float.MAX_VALUE;
@@ -489,7 +492,7 @@ public class CollapsingQParserPlugin extends QParserPlugin {
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
       this.contexts[context.ord] = context;
       this.docBase = context.docBase;
     }
@@ -546,9 +549,9 @@ public class CollapsingQParserPlugin extends QParserPlugin {
       int currentContext = 0;
       int currentDocBase = 0;
       int nextDocBase = currentContext+1 < contexts.length ? contexts[currentContext+1].docBase : maxDoc;
      delegate.setNextReader(contexts[currentContext]);
      leafDelegate = delegate.getLeafCollector(contexts[currentContext]);
       DummyScorer dummy = new DummyScorer();
      delegate.setScorer(dummy);
      leafDelegate.setScorer(dummy);
       DocIdSetIterator it = collapsedSet.iterator();
       int docId = -1;
       int nullScoreIndex = 0;
@@ -571,13 +574,13 @@ public class CollapsingQParserPlugin extends QParserPlugin {
           currentContext++;
           currentDocBase = contexts[currentContext].docBase;
           nextDocBase = currentContext+1 < contexts.length ? contexts[currentContext+1].docBase : maxDoc;
          delegate.setNextReader(contexts[currentContext]);
          delegate.setScorer(dummy);
          leafDelegate = delegate.getLeafCollector(contexts[currentContext]);
          leafDelegate.setScorer(dummy);
         }
 
         int contextDoc = docId-currentDocBase;
         dummy.docId = contextDoc;
        delegate.collect(contextDoc);
        leafDelegate.collect(contextDoc);
       }
 
       if(delegate instanceof DelegatingCollector) {
@@ -590,7 +593,6 @@ public class CollapsingQParserPlugin extends QParserPlugin {
     private AtomicReaderContext[] contexts;
     private SortedDocValues values;
 
    private int docBase;
     private int maxDoc;
     private int nullPolicy;
 
@@ -640,7 +642,7 @@ public class CollapsingQParserPlugin extends QParserPlugin {
       this.fieldValueCollapse.setScorer(scorer);
     }
 
    public void setNextReader(AtomicReaderContext context) throws IOException {
    public void doSetNextReader(AtomicReaderContext context) throws IOException {
       this.contexts[context.ord] = context;
       this.docBase = context.docBase;
       this.fieldValueCollapse.setNextReader(context);
@@ -660,9 +662,9 @@ public class CollapsingQParserPlugin extends QParserPlugin {
       int currentContext = 0;
       int currentDocBase = 0;
       int nextDocBase = currentContext+1 < contexts.length ? contexts[currentContext+1].docBase : maxDoc;
      delegate.setNextReader(contexts[currentContext]);
      leafDelegate = delegate.getLeafCollector(contexts[currentContext]);
       DummyScorer dummy = new DummyScorer();
      delegate.setScorer(dummy);
      leafDelegate.setScorer(dummy);
       DocIdSetIterator it = fieldValueCollapse.getCollapsedSet().iterator();
       int docId = -1;
       int nullScoreIndex = 0;
@@ -689,13 +691,13 @@ public class CollapsingQParserPlugin extends QParserPlugin {
           currentContext++;
           currentDocBase = contexts[currentContext].docBase;
           nextDocBase = currentContext+1 < contexts.length ? contexts[currentContext+1].docBase : maxDoc;
          delegate.setNextReader(contexts[currentContext]);
          delegate.setScorer(dummy);
          leafDelegate = delegate.getLeafCollector(contexts[currentContext]);
          leafDelegate.setScorer(dummy);
         }
 
         int contextDoc = docId-currentDocBase;
         dummy.docId = contextDoc;
        delegate.collect(contextDoc);
        leafDelegate.collect(contextDoc);
       }
 
       if(delegate instanceof DelegatingCollector) {
diff --git a/solr/core/src/java/org/apache/solr/search/DelegatingCollector.java b/solr/core/src/java/org/apache/solr/search/DelegatingCollector.java
index 97045e87d1e..06b96581b8b 100644
-- a/solr/core/src/java/org/apache/solr/search/DelegatingCollector.java
++ b/solr/core/src/java/org/apache/solr/search/DelegatingCollector.java
@@ -18,21 +18,23 @@
 package org.apache.solr.search;
 
 
import org.apache.lucene.index.IndexReader;
import java.io.IOException;

 import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.Scorer;

import java.io.IOException;
import org.apache.lucene.search.SimpleCollector;
 
 
 /** A simple delegating collector where one can set the delegate after creation */
public class DelegatingCollector extends Collector {
public class DelegatingCollector extends SimpleCollector {
 
   /* for internal testing purposes only to determine the number of times a delegating collector chain was used */
   public static int setLastDelegateCount;
 
   protected Collector delegate;
  protected LeafCollector leafDelegate;
   protected Scorer scorer;
   protected AtomicReaderContext context;
   protected int docBase;
@@ -56,24 +58,26 @@ public class DelegatingCollector extends Collector {
   @Override
   public void setScorer(Scorer scorer) throws IOException {
     this.scorer = scorer;
    delegate.setScorer(scorer);
    if (leafDelegate != null) {
      leafDelegate.setScorer(scorer);
    }
   }
 
   @Override
   public void collect(int doc) throws IOException {
    delegate.collect(doc);
    leafDelegate.collect(doc);
   }
 
   @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
  protected void doSetNextReader(AtomicReaderContext context) throws IOException {
     this.context = context;
     this.docBase = context.docBase;
    delegate.setNextReader(context);
    leafDelegate = delegate.getLeafCollector(context);
   }
 
   @Override
   public boolean acceptsDocsOutOfOrder() {
    return delegate.acceptsDocsOutOfOrder();
    return leafDelegate.acceptsDocsOutOfOrder();
   }
 
   public void finish() throws IOException {
diff --git a/solr/core/src/java/org/apache/solr/search/DocSetCollector.java b/solr/core/src/java/org/apache/solr/search/DocSetCollector.java
index 76c3660cce9..cbc179b8232 100644
-- a/solr/core/src/java/org/apache/solr/search/DocSetCollector.java
++ b/solr/core/src/java/org/apache/solr/search/DocSetCollector.java
@@ -20,15 +20,16 @@ package org.apache.solr.search;
 import java.io.IOException;
 
 import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
 import org.apache.lucene.util.FixedBitSet;
 
 /**
  *
  */
 
public class DocSetCollector extends Collector {
public class DocSetCollector extends SimpleCollector {
   int pos=0;
   FixedBitSet bits;
   final int maxDoc;
@@ -84,7 +85,7 @@ public class DocSetCollector extends Collector {
   }
 
   @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
  protected void doSetNextReader(AtomicReaderContext context) throws IOException {
     this.base = context.docBase;
   }
 
diff --git a/solr/core/src/java/org/apache/solr/search/DocSetDelegateCollector.java b/solr/core/src/java/org/apache/solr/search/DocSetDelegateCollector.java
deleted file mode 100644
index a73d77c4958..00000000000
-- a/solr/core/src/java/org/apache/solr/search/DocSetDelegateCollector.java
++ /dev/null
@@ -1,84 +0,0 @@
package org.apache.solr.search;

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

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.util.FixedBitSet;

/**
 *
 */
public class DocSetDelegateCollector extends DocSetCollector {
  final Collector collector;

  public DocSetDelegateCollector(int smallSetSize, int maxDoc, Collector collector) {
    super(smallSetSize, maxDoc);
    this.collector = collector;
  }

  @Override
  public void collect(int doc) throws IOException {
    collector.collect(doc);

    doc += base;
    // optimistically collect the first docs in an array
    // in case the total number will be small enough to represent
    // as a small set like SortedIntDocSet instead...
    // Storing in this array will be quicker to convert
    // than scanning through a potentially huge bit vector.
    // FUTURE: when search methods all start returning docs in order, maybe
    // we could have a ListDocSet() and use the collected array directly.
    if (pos < scratch.length) {
      scratch[pos]=doc;
    } else {
      // this conditional could be removed if BitSet was preallocated, but that
      // would take up more memory, and add more GC time...
      if (bits==null) bits = new FixedBitSet(maxDoc);
      bits.set(doc);
    }

    pos++;
  }

  @Override
  public DocSet getDocSet() {
    if (pos<=scratch.length) {
      // assumes docs were collected in sorted order!
      return new SortedIntDocSet(scratch, pos);
    } else {
      // set the bits for ids that were collected in the array
      for (int i=0; i<scratch.length; i++) bits.set(scratch[i]);
      return new BitDocSet(bits,pos);
    }
  }

  @Override
  public void setScorer(Scorer scorer) throws IOException {
    collector.setScorer(scorer);
  }

  @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
    collector.setNextReader(context);
    this.base = context.docBase;
  }
}
diff --git a/solr/core/src/java/org/apache/solr/search/EarlyTerminatingCollector.java b/solr/core/src/java/org/apache/solr/search/EarlyTerminatingCollector.java
index b9eaca63cea..200d326a350 100644
-- a/solr/core/src/java/org/apache/solr/search/EarlyTerminatingCollector.java
++ b/solr/core/src/java/org/apache/solr/search/EarlyTerminatingCollector.java
@@ -20,67 +20,70 @@ package org.apache.solr.search;
 import java.io.IOException;
 
 import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.FilterLeafCollector;
import org.apache.lucene.search.FilterCollector;
 /**
  * <p>
 *  A wrapper {@link Collector} that throws {@link EarlyTerminatingCollectorException}) 
 *  A wrapper {@link Collector} that throws {@link EarlyTerminatingCollectorException})
  *  once a specified maximum number of documents are collected.
  * </p>
  */
public class EarlyTerminatingCollector extends Collector {
public class EarlyTerminatingCollector extends FilterCollector {
 
   private final int maxDocsToCollect;
  private final Collector delegate;
 
   private int numCollected = 0;
   private int prevReaderCumulativeSize = 0;
  private int currentReaderSize = 0;  
  private int currentReaderSize = 0;
 
   /**
    * <p>
   *  Wraps a {@link Collector}, throwing {@link EarlyTerminatingCollectorException} 
   *  Wraps a {@link Collector}, throwing {@link EarlyTerminatingCollectorException}
    *  once the specified maximum is reached.
    * </p>
    * @param delegate - the Collector to wrap.
    * @param maxDocsToCollect - the maximum number of documents to Collect
   * 
   *
    */
   public EarlyTerminatingCollector(Collector delegate, int maxDocsToCollect) {
    super(delegate);
     assert 0 < maxDocsToCollect;
     assert null != delegate;
 
    this.delegate = delegate;
     this.maxDocsToCollect = maxDocsToCollect;
   }
 
  /**
   * This collector requires that docs be collected in order, otherwise
   * the computed number of scanned docs in the resulting 
   * {@link EarlyTerminatingCollectorException} will be meaningless.
   */
  @Override
  public boolean acceptsDocsOutOfOrder() {
    return false;
  }

  @Override
  public void collect(int doc) throws IOException {
    delegate.collect(doc);
    numCollected++;  
    if(maxDocsToCollect <= numCollected) {
      throw new EarlyTerminatingCollectorException
        (numCollected, prevReaderCumulativeSize + (doc + 1));
    }
  }
   @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
  public LeafCollector getLeafCollector(AtomicReaderContext context)
      throws IOException {
     prevReaderCumulativeSize += currentReaderSize; // not current any more
     currentReaderSize = context.reader().maxDoc() - 1;
    delegate.setNextReader(context);
  }
  @Override
  public void setScorer(Scorer scorer) throws IOException {
    delegate.setScorer(scorer);    

    return new FilterLeafCollector(super.getLeafCollector(context)) {

      /**
       * This collector requires that docs be collected in order, otherwise
       * the computed number of scanned docs in the resulting
       * {@link EarlyTerminatingCollectorException} will be meaningless.
       */
      @Override
      public boolean acceptsDocsOutOfOrder() {
        return false;
      }

      @Override
      public void collect(int doc) throws IOException {
        super.collect(doc);
        numCollected++;
        if (maxDocsToCollect <= numCollected) {
          throw new EarlyTerminatingCollectorException
            (numCollected, prevReaderCumulativeSize + (doc + 1));
        }
      }

    };
   }

 }
diff --git a/solr/core/src/java/org/apache/solr/search/FunctionRangeQuery.java b/solr/core/src/java/org/apache/solr/search/FunctionRangeQuery.java
index 4e913dd55b3..91bc1c01240 100644
-- a/solr/core/src/java/org/apache/solr/search/FunctionRangeQuery.java
++ b/solr/core/src/java/org/apache/solr/search/FunctionRangeQuery.java
@@ -22,6 +22,9 @@ import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.queries.function.FunctionValues;
 import org.apache.lucene.queries.function.ValueSource;
 import org.apache.lucene.queries.function.ValueSourceScorer;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.FilterCollector;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.solr.search.function.ValueSourceRangeFilter;
 
@@ -55,16 +58,16 @@ public class FunctionRangeQuery extends SolrConstantScoreQuery implements PostFi
     @Override
     public void collect(int doc) throws IOException {
       if (doc<maxdoc && scorer.matches(doc)) {
        delegate.collect(doc);
        leafDelegate.collect(doc);
       }
     }
 
     @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
    protected void doSetNextReader(AtomicReaderContext context) throws IOException {
      super.doSetNextReader(context);
       maxdoc = context.reader().maxDoc();
       FunctionValues dv = rangeFilt.getValueSource().getValues(fcontext, context);
       scorer = dv.getRangeScorer(context.reader(), rangeFilt.getLowerVal(), rangeFilt.getUpperVal(), rangeFilt.isIncludeLower(), rangeFilt.isIncludeUpper());
      super.setNextReader(context);
     }
   }
 }
diff --git a/solr/core/src/java/org/apache/solr/search/Grouping.java b/solr/core/src/java/org/apache/solr/search/Grouping.java
index 88066de69c8..ee12482c361 100644
-- a/solr/core/src/java/org/apache/solr/search/Grouping.java
++ b/solr/core/src/java/org/apache/solr/search/Grouping.java
@@ -342,12 +342,12 @@ public class Grouping {
       }
     }
 
    Collector allCollectors = MultiCollector.wrap(collectors.toArray(new Collector[collectors.size()]));
     DocSetCollector setCollector = null;
     if (getDocSet && allGroupHeadsCollector == null) {
      setCollector = new DocSetDelegateCollector(maxDoc >> 6, maxDoc, allCollectors);
      allCollectors = setCollector;
      setCollector = new DocSetCollector(maxDoc >> 6, maxDoc);
      collectors.add(setCollector);
     }
    Collector allCollectors = MultiCollector.wrap(collectors);
 
     CachingCollector cachedCollector = null;
     if (cacheSecondPassSearch && allCollectors != null) {
diff --git a/solr/core/src/java/org/apache/solr/search/SolrIndexSearcher.java b/solr/core/src/java/org/apache/solr/search/SolrIndexSearcher.java
index be97d81e9d6..176c0df733f 100644
-- a/solr/core/src/java/org/apache/solr/search/SolrIndexSearcher.java
++ b/solr/core/src/java/org/apache/solr/search/SolrIndexSearcher.java
@@ -60,6 +60,7 @@ import org.apache.lucene.index.StoredFieldVisitor;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.Terms;
 import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.Collector;
@@ -71,9 +72,11 @@ import org.apache.lucene.search.FieldDoc;
 import org.apache.lucene.search.Filter;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiCollector;
 import org.apache.lucene.search.Query;
 import org.apache.lucene.search.ScoreDoc;
 import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
 import org.apache.lucene.search.Sort;
 import org.apache.lucene.search.SortField;
 import org.apache.lucene.search.TermQuery;
@@ -930,17 +933,17 @@ public class SolrIndexSearcher extends IndexSearcher implements Closeable,SolrIn
         if (idIter == null) continue;
       }
 
      collector.setNextReader(leaf);
      final LeafCollector leafCollector = collector.getLeafCollector(leaf);
       int max = reader.maxDoc();
 
       if (idIter == null) {
         for (int docid = 0; docid<max; docid++) {
           if (liveDocs != null && !liveDocs.get(docid)) continue;
          collector.collect(docid);
          leafCollector.collect(docid);
         }
       } else {
         for (int docid = -1; (docid = idIter.advance(docid+1)) < max; ) {
          collector.collect(docid);
          leafCollector.collect(docid);
         }
       }
     }
@@ -1526,24 +1529,18 @@ public class SolrIndexSearcher extends IndexSearcher implements Closeable,SolrIn
       Collector collector;
 
       if (!needScores) {
        collector = new Collector () {
          @Override
          public void setScorer(Scorer scorer) {
          }
        collector = new SimpleCollector () {
           @Override
           public void collect(int doc) {
             numHits[0]++;
           }
           @Override
          public void setNextReader(AtomicReaderContext context) {
          }
          @Override
           public boolean acceptsDocsOutOfOrder() {
             return true;
           }
         };
       } else {
        collector = new Collector() {
        collector = new SimpleCollector() {
           Scorer scorer;
           @Override
           public void setScorer(Scorer scorer) {
@@ -1556,9 +1553,6 @@ public class SolrIndexSearcher extends IndexSearcher implements Closeable,SolrIn
             if (score > topscore[0]) topscore[0]=score;            
           }
           @Override
          public void setNextReader(AtomicReaderContext context) {
          }
          @Override
           public boolean acceptsDocsOutOfOrder() {
             return true;
           }
@@ -1667,30 +1661,33 @@ public class SolrIndexSearcher extends IndexSearcher implements Closeable,SolrIn
       final float[] topscore = new float[] { Float.NEGATIVE_INFINITY };
 
       Collector collector;
      DocSetCollector setCollector;
      final DocSetCollector setCollector = new DocSetCollector(smallSetSize, maxDoc);
 
        if (!needScores) {
         collector = setCollector = new DocSetCollector(smallSetSize, maxDoc);
         collector = setCollector;
        } else {
         collector = setCollector = new DocSetDelegateCollector(smallSetSize, maxDoc, new Collector() {
         final Collector topScoreCollector = new SimpleCollector() {
          
            Scorer scorer;
           
            @Override
          public void setScorer(Scorer scorer) {
             this.scorer = scorer;
           }
           @Override
          public void setScorer(Scorer scorer) throws IOException {
            this.scorer = scorer;
          }
           
          @Override
           public void collect(int doc) throws IOException {
             float score = scorer.score();
             if (score > topscore[0]) topscore[0]=score;
           }
           @Override
          public void setNextReader(AtomicReaderContext context) {
           }
           @Override
            float score = scorer.score();
            if (score > topscore[0]) topscore[0] = score;
          }
          
          @Override
           public boolean acceptsDocsOutOfOrder() {
             return false;
           }
         });
            return true;
          }
        };
        
        collector = MultiCollector.wrap(setCollector, topScoreCollector);
        }
        if (terminateEarly) {
          collector = new EarlyTerminatingCollector(collector, cmd.len);
@@ -1726,8 +1723,8 @@ public class SolrIndexSearcher extends IndexSearcher implements Closeable,SolrIn
     } else {
 
       final TopDocsCollector topCollector = buildTopDocsCollector(len, cmd);
      DocSetCollector setCollector = new DocSetDelegateCollector(maxDoc>>6, maxDoc, topCollector);
      Collector collector = setCollector;
      DocSetCollector setCollector = new DocSetCollector(maxDoc>>6, maxDoc);
      Collector collector = MultiCollector.wrap(topCollector, setCollector);
       if (terminateEarly) {
         collector = new EarlyTerminatingCollector(collector, cmd.len);
       }
@@ -2031,7 +2028,7 @@ public class SolrIndexSearcher extends IndexSearcher implements Closeable,SolrIn
         AtomicReaderContext leaf = leafContexts.get(readerIndex++);
         base = leaf.docBase;
         end = base + leaf.reader().maxDoc();
        topCollector.setNextReader(leaf);
        topCollector.getLeafCollector(leaf);
         // we should never need to set the scorer given the settings for the collector
       }
       topCollector.collect(doc-base);
diff --git a/solr/core/src/java/org/apache/solr/search/grouping/CommandHandler.java b/solr/core/src/java/org/apache/solr/search/grouping/CommandHandler.java
index 4d10c931337..e8425138b68 100644
-- a/solr/core/src/java/org/apache/solr/search/grouping/CommandHandler.java
++ b/solr/core/src/java/org/apache/solr/search/grouping/CommandHandler.java
@@ -29,12 +29,10 @@ import org.apache.lucene.search.TimeLimitingCollector;
 import org.apache.lucene.search.TotalHitCountCollector;
 import org.apache.lucene.search.grouping.AbstractAllGroupHeadsCollector;
 import org.apache.lucene.search.grouping.term.TermAllGroupHeadsCollector;
import org.apache.lucene.util.FixedBitSet;
 import org.apache.solr.common.util.NamedList;
 import org.apache.solr.search.BitDocSet;
 import org.apache.solr.search.DocSet;
 import org.apache.solr.search.DocSetCollector;
import org.apache.solr.search.DocSetDelegateCollector;
 import org.apache.solr.search.QueryUtils;
 import org.apache.solr.search.SolrIndexSearcher;
 import org.apache.solr.search.SolrIndexSearcher.ProcessedFilter;
@@ -173,14 +171,11 @@ public class CommandHandler {
 
   private DocSet computeDocSet(Query query, ProcessedFilter filter, List<Collector> collectors) throws IOException {
     int maxDoc = searcher.maxDoc();
    DocSetCollector docSetCollector;
    if (collectors.isEmpty()) {
      docSetCollector = new DocSetCollector(maxDoc >> 6, maxDoc);
    } else {
      Collector wrappedCollectors = MultiCollector.wrap(collectors.toArray(new Collector[collectors.size()]));
      docSetCollector = new DocSetDelegateCollector(maxDoc >> 6, maxDoc, wrappedCollectors);
    }
    searchWithTimeLimiter(query, filter, docSetCollector);
    final Collector collector;
    final DocSetCollector docSetCollector = new DocSetCollector(maxDoc >> 6, maxDoc);
    List<Collector> allCollectors = new ArrayList<>(collectors);
    allCollectors.add(docSetCollector);
    searchWithTimeLimiter(query, filter, MultiCollector.wrap(allCollectors));
     return docSetCollector.getDocSet();
   }
 
diff --git a/solr/core/src/java/org/apache/solr/search/grouping/collector/FilterCollector.java b/solr/core/src/java/org/apache/solr/search/grouping/collector/FilterCollector.java
index 5ab7f1824be..3dd854525ab 100644
-- a/solr/core/src/java/org/apache/solr/search/grouping/collector/FilterCollector.java
++ b/solr/core/src/java/org/apache/solr/search/grouping/collector/FilterCollector.java
@@ -17,52 +17,42 @@ package org.apache.solr.search.grouping.collector;
  * limitations under the License.
  */
 
import java.io.IOException;

 import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.FilterLeafCollector;
 import org.apache.solr.search.DocSet;
 
import java.io.IOException;

 /**
  * A collector that filters incoming doc ids that are not in the filter.
  *
  * @lucene.experimental
  */
public class FilterCollector extends Collector {
public class FilterCollector extends org.apache.lucene.search.FilterCollector {
 
   private final DocSet filter;
  private final Collector delegate;
  private int docBase;
   private int matches;
 
   public FilterCollector(DocSet filter, Collector delegate) {
    super(delegate);
     this.filter = filter;
    this.delegate = delegate;
  }

  @Override
  public void setScorer(Scorer scorer) throws IOException {
    delegate.setScorer(scorer);
  }

  @Override
  public void collect(int doc) throws IOException {
    matches++;
    if (filter.exists(doc + docBase)) {
      delegate.collect(doc);
    }
  }

  @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
    this.docBase = context.docBase;
    delegate.setNextReader(context);
   }
 
   @Override
  public boolean acceptsDocsOutOfOrder() {
    return delegate.acceptsDocsOutOfOrder();
  public LeafCollector getLeafCollector(AtomicReaderContext context)
      throws IOException {
    final int docBase = context.docBase;
    return new FilterLeafCollector(super.getLeafCollector(context)) {
      @Override
      public void collect(int doc) throws IOException {
        matches++;
        if (filter.exists(doc + docBase)) {
          super.collect(doc);
        }
      }
    };
   }
 
   public int getMatches() {
@@ -75,6 +65,6 @@ public class FilterCollector extends Collector {
    * @return the delegate collector
    */
   public Collector getDelegate() {
    return delegate;
    return in;
   }
 }
diff --git a/solr/core/src/test/org/apache/solr/search/TestSort.java b/solr/core/src/test/org/apache/solr/search/TestSort.java
index b31952191b3..9671374bde5 100644
-- a/solr/core/src/test/org/apache/solr/search/TestSort.java
++ b/solr/core/src/test/org/apache/solr/search/TestSort.java
@@ -32,10 +32,13 @@ import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.DirectoryReader;
 import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.BitsFilteredDocIdSet;
 import org.apache.lucene.search.Collector;
 import org.apache.lucene.search.DocIdSet;
 import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilterLeafCollector;
import org.apache.lucene.search.FilterCollector;
 import org.apache.lucene.search.IndexSearcher;
 import org.apache.lucene.search.MatchAllDocsQuery;
 import org.apache.lucene.search.ScoreDoc;
@@ -265,30 +268,21 @@ public class TestSort extends SolrTestCaseJ4 {
 
         final List<MyDoc> collectedDocs = new ArrayList<>();
         // delegate and collect docs ourselves
        Collector myCollector = new Collector() {
          int docBase;
        Collector myCollector = new FilterCollector(topCollector) {
 
           @Override
          public void setScorer(Scorer scorer) throws IOException {
            topCollector.setScorer(scorer);
          public LeafCollector getLeafCollector(AtomicReaderContext context)
              throws IOException {
            final int docBase = context.docBase;
            return new FilterLeafCollector(super.getLeafCollector(context)) {
              @Override
              public void collect(int doc) throws IOException {
                super.collect(doc);
                collectedDocs.add(mydocs[docBase + doc]);
              }
            };
           }
 
          @Override
          public void collect(int doc) throws IOException {
            topCollector.collect(doc);
            collectedDocs.add(mydocs[doc + docBase]);
          }

          @Override
          public void setNextReader(AtomicReaderContext context) throws IOException {
            topCollector.setNextReader(context);
            docBase = context.docBase;
          }

          @Override
          public boolean acceptsDocsOutOfOrder() {
            return topCollector.acceptsDocsOutOfOrder();
          }
         };
 
         searcher.search(new MatchAllDocsQuery(), filt, myCollector);
- 
2.19.1.windows.1

