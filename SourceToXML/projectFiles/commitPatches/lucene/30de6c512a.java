From 30de6c512ae51391159b28e0250a9482f12690c7 Mon Sep 17 00:00:00 2001
From: Robert Muir <rmuir@apache.org>
Date: Mon, 21 Apr 2014 18:36:34 +0000
Subject: [PATCH] LUCENE-5623: fix bug in earlyterminatingcollector, fix test
 to be reproducible and more evil

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1588953 13f79535-47bb-0310-9956-ffa450edef68
--
 .../EarlyTerminatingSortingCollector.java     |  3 +-
 .../index/sorter/TestEarlyTermination.java    | 98 +++++++++++--------
 .../index/sorter/TestSortingMergePolicy.java  | 10 +-
 3 files changed, 67 insertions(+), 44 deletions(-)

diff --git a/lucene/misc/src/java/org/apache/lucene/index/sorter/EarlyTerminatingSortingCollector.java b/lucene/misc/src/java/org/apache/lucene/index/sorter/EarlyTerminatingSortingCollector.java
index 2571632defd..27f61adc8e3 100644
-- a/lucene/misc/src/java/org/apache/lucene/index/sorter/EarlyTerminatingSortingCollector.java
++ b/lucene/misc/src/java/org/apache/lucene/index/sorter/EarlyTerminatingSortingCollector.java
@@ -70,8 +70,6 @@ public class EarlyTerminatingSortingCollector extends FilterCollector {
   /** Number of documents to collect in each segment */
   protected final int numDocsToCollect;
 
  private int numCollected;

   /**
    * Create a new {@link EarlyTerminatingSortingCollector} instance.
    *
@@ -98,6 +96,7 @@ public class EarlyTerminatingSortingCollector extends FilterCollector {
     if (SortingMergePolicy.isSorted(context.reader(), sort)) {
       // segment is sorted, can early-terminate
       return new FilterLeafCollector(super.getLeafCollector(context)) {
        private int numCollected;
 
         @Override
         public void collect(int doc) throws IOException {
diff --git a/lucene/misc/src/test/org/apache/lucene/index/sorter/TestEarlyTermination.java b/lucene/misc/src/test/org/apache/lucene/index/sorter/TestEarlyTermination.java
index e1922ae9377..3af59286ab4 100644
-- a/lucene/misc/src/test/org/apache/lucene/index/sorter/TestEarlyTermination.java
++ b/lucene/misc/src/test/org/apache/lucene/index/sorter/TestEarlyTermination.java
@@ -33,9 +33,12 @@ import org.apache.lucene.index.AtomicReaderContext;
 import org.apache.lucene.index.IndexReader;
 import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.SerialMergeScheduler;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.search.LeafCollector;
 import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
 import org.apache.lucene.search.ScoreDoc;
 import org.apache.lucene.search.Sort;
 import org.apache.lucene.search.SortField;
@@ -70,7 +73,7 @@ public class TestEarlyTermination extends LuceneTestCase {
     return doc;
   }
 
  private void createRandomIndexes(int maxSegments) throws IOException {
  private void createRandomIndex() throws IOException {
     dir = newDirectory();
     numDocs = atLeast(150);
     final int numTerms = TestUtil.nextInt(random(), 1, numDocs / 5);
@@ -81,8 +84,10 @@ public class TestEarlyTermination extends LuceneTestCase {
     terms = new ArrayList<>(randomTerms);
     final long seed = random().nextLong();
     final IndexWriterConfig iwc = newIndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(new Random(seed)));
    iwc.setMergeScheduler(new SerialMergeScheduler()); // for reproducible tests
     iwc.setMergePolicy(TestSortingMergePolicy.newSortingMergePolicy(sort));
     iw = new RandomIndexWriter(new Random(seed), dir, iwc);
    iw.setDoRandomForceMerge(false); // don't do this, it may happen anyway with MockRandomMP
     for (int i = 0; i < numDocs; ++i) {
       final Document doc = randomDocument();
       iw.addDocument(doc);
@@ -94,56 +99,70 @@ public class TestEarlyTermination extends LuceneTestCase {
         iw.deleteDocuments(new Term("s", term));
       }
     }
    if (random().nextBoolean()) {
      iw.forceMerge(5);
    }
     reader = iw.getReader();
   }

  @Override
  public void tearDown() throws Exception {
  
  private void closeIndex() throws IOException {
     reader.close();
     iw.shutdown();
     dir.close();
    super.tearDown();
   }
 
   public void testEarlyTermination() throws IOException {
    createRandomIndexes(5);
    final int numHits = TestUtil.nextInt(random(), 1, numDocs / 10);
    final Sort sort = new Sort(new SortField("ndv1", SortField.Type.LONG, false));
    final boolean fillFields = random().nextBoolean();
    final boolean trackDocScores = random().nextBoolean();
    final boolean trackMaxScore = random().nextBoolean();
    final boolean inOrder = random().nextBoolean();
    final TopFieldCollector collector1 = TopFieldCollector.create(sort, numHits, fillFields, trackDocScores, trackMaxScore, inOrder);
    final TopFieldCollector collector2 = TopFieldCollector.create(sort, numHits, fillFields, trackDocScores, trackMaxScore, inOrder);

    final IndexSearcher searcher = newSearcher(reader);
    final int iters = atLeast(5);
    final int iters = atLeast(8);
     for (int i = 0; i < iters; ++i) {
      final TermQuery query = new TermQuery(new Term("s", RandomPicks.randomFrom(random(), terms)));
      searcher.search(query, collector1);
      searcher.search(query, new EarlyTerminatingSortingCollector(collector2, sort, numHits));
      createRandomIndex();
      for (int j = 0; j < iters; ++j) {
        final IndexSearcher searcher = newSearcher(reader);
        final int numHits = TestUtil.nextInt(random(), 1, numDocs);
        final Sort sort = new Sort(new SortField("ndv1", SortField.Type.LONG, false));
        final boolean fillFields = random().nextBoolean();
        final boolean trackDocScores = random().nextBoolean();
        final boolean trackMaxScore = random().nextBoolean();
        final boolean inOrder = random().nextBoolean();
        final TopFieldCollector collector1 = TopFieldCollector.create(sort, numHits, fillFields, trackDocScores, trackMaxScore, inOrder);
        final TopFieldCollector collector2 = TopFieldCollector.create(sort, numHits, fillFields, trackDocScores, trackMaxScore, inOrder);

        final Query query;
        if (random().nextBoolean()) {
          query = new TermQuery(new Term("s", RandomPicks.randomFrom(random(), terms)));
        } else {
          query = new MatchAllDocsQuery();
        }
        searcher.search(query, collector1);
        searcher.search(query, new EarlyTerminatingSortingCollector(collector2, sort, numHits));
        assertTrue(collector1.getTotalHits() >= collector2.getTotalHits());
        assertTopDocsEquals(collector1.topDocs().scoreDocs, collector2.topDocs().scoreDocs);
      }
      closeIndex();
     }
    assertTrue(collector1.getTotalHits() >= collector2.getTotalHits());
    assertTopDocsEquals(collector1.topDocs().scoreDocs, collector2.topDocs().scoreDocs);
   }
   
   public void testEarlyTerminationDifferentSorter() throws IOException {
    // test that the collector works correctly when the index was sorted by a
    // different sorter than the one specified in the ctor.
    createRandomIndexes(5);
    final int numHits = TestUtil.nextInt(random(), 1, numDocs / 10);
    final Sort sort = new Sort(new SortField("ndv2", SortField.Type.LONG, false));
    final boolean fillFields = random().nextBoolean();
    final boolean trackDocScores = random().nextBoolean();
    final boolean trackMaxScore = random().nextBoolean();
    final boolean inOrder = random().nextBoolean();
    final TopFieldCollector collector1 = TopFieldCollector.create(sort, numHits, fillFields, trackDocScores, trackMaxScore, inOrder);
    final TopFieldCollector collector2 = TopFieldCollector.create(sort, numHits, fillFields, trackDocScores, trackMaxScore, inOrder);
    
    final IndexSearcher searcher = newSearcher(reader);
    final int iters = atLeast(5);
    createRandomIndex();
    final int iters = atLeast(3);
     for (int i = 0; i < iters; ++i) {
      final TermQuery query = new TermQuery(new Term("s", RandomPicks.randomFrom(random(), terms)));
      final IndexSearcher searcher = newSearcher(reader);
      // test that the collector works correctly when the index was sorted by a
      // different sorter than the one specified in the ctor.
      final int numHits = TestUtil.nextInt(random(), 1, numDocs);
      final Sort sort = new Sort(new SortField("ndv2", SortField.Type.LONG, false));
      final boolean fillFields = random().nextBoolean();
      final boolean trackDocScores = random().nextBoolean();
      final boolean trackMaxScore = random().nextBoolean();
      final boolean inOrder = random().nextBoolean();
      final TopFieldCollector collector1 = TopFieldCollector.create(sort, numHits, fillFields, trackDocScores, trackMaxScore, inOrder);
      final TopFieldCollector collector2 = TopFieldCollector.create(sort, numHits, fillFields, trackDocScores, trackMaxScore, inOrder);
      
      final Query query;
      if (random().nextBoolean()) {
        query = new TermQuery(new Term("s", RandomPicks.randomFrom(random(), terms)));
      } else {
        query = new MatchAllDocsQuery();
      }
       searcher.search(query, collector1);
       Sort different = new Sort(new SortField("ndv2", SortField.Type.LONG));
       searcher.search(query, new EarlyTerminatingSortingCollector(collector2, different, numHits) {
@@ -154,9 +173,10 @@ public class TestEarlyTermination extends LuceneTestCase {
           return ret;
         }
       });
      assertTrue(collector1.getTotalHits() >= collector2.getTotalHits());
      assertTopDocsEquals(collector1.topDocs().scoreDocs, collector2.topDocs().scoreDocs);
     }
    assertTrue(collector1.getTotalHits() >= collector2.getTotalHits());
    assertTopDocsEquals(collector1.topDocs().scoreDocs, collector2.topDocs().scoreDocs);
    closeIndex();
   }
 
   private static void assertTopDocsEquals(ScoreDoc[] scoreDocs1, ScoreDoc[] scoreDocs2) {
diff --git a/lucene/misc/src/test/org/apache/lucene/index/sorter/TestSortingMergePolicy.java b/lucene/misc/src/test/org/apache/lucene/index/sorter/TestSortingMergePolicy.java
index b193bdd875d..65d47641bd1 100644
-- a/lucene/misc/src/test/org/apache/lucene/index/sorter/TestSortingMergePolicy.java
++ b/lucene/misc/src/test/org/apache/lucene/index/sorter/TestSortingMergePolicy.java
@@ -71,18 +71,22 @@ public class TestSortingMergePolicy extends LuceneTestCase {
   }
 
   static MergePolicy newSortingMergePolicy(Sort sort) {
    // create a MP with a low merge factor so that many merges happen
    // usually create a MP with a low merge factor so that many merges happen
     MergePolicy mp;
    if (random().nextBoolean()) {
    int thingToDo = random().nextInt(3);
    if (thingToDo == 0) {
       TieredMergePolicy tmp = newTieredMergePolicy(random());
       final int numSegs = TestUtil.nextInt(random(), 3, 5);
       tmp.setSegmentsPerTier(numSegs);
       tmp.setMaxMergeAtOnce(TestUtil.nextInt(random(), 2, numSegs));
       mp = tmp;
    } else {
    } else if (thingToDo == 1) {
       LogMergePolicy lmp = newLogMergePolicy(random());
       lmp.setMergeFactor(TestUtil.nextInt(random(), 3, 5));
       mp = lmp;
    } else {
      // just a regular random one from LTC (could be alcoholic etc)
      mp = newMergePolicy();
     }
     // wrap it with a sorting mp
     return new SortingMergePolicy(mp, sort);
- 
2.19.1.windows.1

